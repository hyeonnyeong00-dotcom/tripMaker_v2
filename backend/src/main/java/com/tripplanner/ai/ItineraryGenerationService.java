package com.tripplanner.ai;

import com.tripplanner.ai.dto.AiActivityPayload;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiItineraryPayload;
import com.tripplanner.cache.AiResponseCacheService;
import com.tripplanner.cache.CacheKeyGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 최초 생성(§5.3/5.4) 오케스트레이션: 캐시 조회 → 미스 시 프롬프트 렌더링 → AI 호출(재시도) →
 * 파싱/검증(재시도) → 캐시 upsert. Trip/ItineraryDay/ItineraryActivity 영속화는 trip 모듈 책임.
 */
@Service
public class ItineraryGenerationService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryGenerationService.class);

    private static final String SYSTEM_PROMPT =
            "너는 여행 일정 플래너 AI다. 반드시 유효한 JSON 객체 하나만 출력하고, "
                    + "설명 문장이나 마크다운 코드펜스(```)는 절대 포함하지 마라.";

    private static final int MAX_ATTEMPTS = 2;
    // 실측(나고야 4일, haiku): activity 1개 ≈ 240토큰 × 하루 4~6개 ≈ 일당 1,200토큰 소비.
    // 일당 500(→900)이었을 때 응답이 max_tokens에서 잘려 미완성 JSON 파싱 실패가 재현됨 → 여유분 포함 1,600/일.
    // (V7 다이어트로 실사용량은 줄지만 max_tokens는 상한일 뿐 비용에 영향 없으므로 보수적으로 유지)
    private static final int BASE_MAX_TOKENS = 1500;
    private static final int TOKENS_PER_DAY = 1600;
    // 실측 출력 속도 ≈ 155tok/s → 토큰 예산에 비례해 타임아웃도 상향
    private static final int BASE_TIMEOUT_SECONDS = 20;
    private static final int TIMEOUT_SECONDS_PER_DAY = 15;
    private static final int MAX_TIMEOUT_SECONDS = 360;
    // 1분 내 생성 목표: 이 일수를 넘으면 구간을 나눠 병렬 생성 후 병합한다
    private static final int SINGLE_CALL_MAX_DAYS = 7;
    private static final int MAX_DAYS_PER_CHUNK = 6;

    // 청크 병렬 호출용 공용 풀. 단일 인스턴스 전제(§1)라 최대 동시 호출만 제한한다.
    // 애플리케이션 수명과 동일하게 살아 있고 요청 간 재사용되므로 별도 shutdown 훅은 두지 않는다
    // (JVM 종료 시 함께 정리; graceful shutdown이 필요해지면 @PreDestroy로 shutdown 추가할 것).
    private final ExecutorService chunkExecutor = Executors.newFixedThreadPool(5);

    private final PromptTemplateService promptTemplateService;
    private final InitialGenerationPromptRenderer promptRenderer;
    private final AnthropicClient anthropicClient;
    private final CacheKeyGenerator cacheKeyGenerator;
    private final AiResponseCacheService cacheService;
    private final AiItineraryPayloadParser payloadParser;
    private final AiUsageLogger aiUsageLogger;

    public ItineraryGenerationService(
            PromptTemplateService promptTemplateService,
            InitialGenerationPromptRenderer promptRenderer,
            AnthropicClient anthropicClient,
            CacheKeyGenerator cacheKeyGenerator,
            AiResponseCacheService cacheService,
            AiItineraryPayloadParser payloadParser,
            AiUsageLogger aiUsageLogger) {
        this.promptTemplateService = promptTemplateService;
        this.promptRenderer = promptRenderer;
        this.anthropicClient = anthropicClient;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.cacheService = cacheService;
        this.payloadParser = payloadParser;
        this.aiUsageLogger = aiUsageLogger;
    }

    public AiItineraryPayload generate(
            String destination,
            int durationDays,
            Integer budgetMin,
            Integer budgetMax,
            String companion,
            List<String> preferences,
            boolean includeNearby,
            String activeStartTime,
            String activeEndTime) {
        String cacheKey = cacheKeyGenerator.generate(
                destination,
                durationDays,
                budgetMin,
                budgetMax,
                companion,
                preferences,
                includeNearby,
                activeStartTime,
                activeEndTime);

        Optional<AiItineraryPayload> cached = cacheService.lookup(cacheKey);
        if (cached.isPresent()) {
            log.info("AI 응답 캐시 히트, AI 미호출. cacheKey={}", cacheKey);
            // 초기 생성은 이 시점에 trip이 아직 없어 trip_id는 "-"로 기록한다
            aiUsageLogger.logCacheHit(null);
            return cached.get();
        }
        log.info("AI 응답 캐시 미스, AI 직접 호출 진행. cacheKey={}", cacheKey);

        PromptTemplate template = promptTemplateService.loadActive("initial_generation");
        AiItineraryPayload payload = durationDays <= SINGLE_CALL_MAX_DAYS
                ? generateSegment(template, destination, 1, durationDays, durationDays,
                        budgetMin, budgetMax, companion, preferences, includeNearby, activeStartTime, activeEndTime)
                : generateChunked(template, destination, durationDays,
                        budgetMin, budgetMax, companion, preferences, includeNearby, activeStartTime, activeEndTime);
        payload = clampActivityTimes(payload, activeStartTime, activeEndTime);

        Map<String, Object> requestParams = new LinkedHashMap<>();
        requestParams.put("destination", destination);
        requestParams.put("duration_days", durationDays);
        requestParams.put("budget_min", budgetMin);
        requestParams.put("budget_max", budgetMax);
        requestParams.put("companion", companion);
        requestParams.put("preferences", preferences);
        requestParams.put("include_nearby", includeNearby);
        requestParams.put("active_start_time", activeStartTime);
        requestParams.put("active_end_time", activeEndTime);
        cacheService.upsert(cacheKey, requestParams, payload);

        return payload;
    }

    /** [startDay, endDay] 구간을 한 번의 AI 호출로 생성한다(전체 생성이면 1~durationDays). */
    private AiItineraryPayload generateSegment(
            PromptTemplate template,
            String destination,
            int startDay,
            int endDay,
            int totalDays,
            Integer budgetMin,
            Integer budgetMax,
            String companion,
            List<String> preferences,
            boolean includeNearby,
            String activeStartTime,
            String activeEndTime) {
        int segmentDays = endDay - startDay + 1;
        String userPrompt = promptRenderer.render(
                template, destination, startDay, endDay, totalDays,
                budgetMin, budgetMax, companion, preferences, includeNearby, activeStartTime, activeEndTime);
        int maxTokens = BASE_MAX_TOKENS + TOKENS_PER_DAY * segmentDays;
        Duration timeout = Duration.ofSeconds(
                Math.min(MAX_TIMEOUT_SECONDS, BASE_TIMEOUT_SECONDS + TIMEOUT_SECONDS_PER_DAY * segmentDays));
        return callAndParseWithRetry(userPrompt, maxTokens, timeout, segmentDays,
                template.getName(), template.getVersion());
    }

    /**
     * SINGLE_CALL_MAX_DAYS 초과 일정은 MAX_DAYS_PER_CHUNK 이하 구간으로 균등 분할해 병렬 생성 후
     * day 번호/activity id를 위치 기반으로 재부여하며 병합한다(§5.4 "1분 내 생성" 개선).
     */
    private AiItineraryPayload generateChunked(
            PromptTemplate template,
            String destination,
            int durationDays,
            Integer budgetMin,
            Integer budgetMax,
            String companion,
            List<String> preferences,
            boolean includeNearby,
            String activeStartTime,
            String activeEndTime) {
        List<int[]> ranges = splitRanges(durationDays);
        log.info("장기 일정 분할 생성: durationDays={} chunks={}", durationDays, ranges.size());

        List<CompletableFuture<AiItineraryPayload>> futures = ranges.stream()
                .map(range -> CompletableFuture.supplyAsync(
                        () -> generateSegment(template, destination, range[0], range[1], durationDays,
                                budgetMin, budgetMax, companion, preferences, includeNearby,
                                activeStartTime, activeEndTime),
                        chunkExecutor))
                .toList();

        List<AiItineraryPayload> chunks = new ArrayList<>();
        try {
            for (CompletableFuture<AiItineraryPayload> future : futures) {
                chunks.add(future.join());
            }
        } catch (CompletionException e) {
            if (e.getCause() instanceof AiCallException callException) {
                throw callException;
            }
            if (e.getCause() instanceof AiParseException parseException) {
                throw parseException;
            }
            throw new AiCallException("분할 생성 중 알 수 없는 실패", e.getCause());
        }

        return mergeChunks(destination, durationDays, ranges, chunks);
    }

    /** durationDays를 MAX_DAYS_PER_CHUNK 이하의 균등한 [startDay, endDay] 구간들로 나눈다. */
    private List<int[]> splitRanges(int durationDays) {
        int chunkCount = (durationDays + MAX_DAYS_PER_CHUNK - 1) / MAX_DAYS_PER_CHUNK;
        int baseSize = durationDays / chunkCount;
        int remainder = durationDays % chunkCount;
        List<int[]> ranges = new ArrayList<>();
        int start = 1;
        for (int i = 0; i < chunkCount; i++) {
            int size = baseSize + (i < remainder ? 1 : 0);
            ranges.add(new int[] {start, start + size - 1});
            start += size;
        }
        return ranges;
    }

    /**
     * §5.4 "활동 시간은 active_start~end 범위 안" 보정: 프롬프트로 강제해도 모델이 간혹
     * 범위 밖(주로 이른 아침) 시작 시간을 내므로, 범위 밖 time은 경계값으로 클램프한다.
     */
    private AiItineraryPayload clampActivityTimes(AiItineraryPayload payload, String activeStartTime, String activeEndTime) {
        List<AiDayPayload> fixedDays = new ArrayList<>();
        for (AiDayPayload day : payload.days()) {
            List<AiActivityPayload> fixedActivities = new ArrayList<>();
            for (AiActivityPayload act : day.activities()) {
                String time = act.time();
                // time/activeStart/activeEnd 모두 "HH:mm" 24시간 표기라 사전식(String.compareTo) 비교가
                // 곧 시간 순서 비교와 일치한다(자정을 넘기는 일정은 없다는 전제 — active_end는 항상 같은 날).
                if (time != null && time.compareTo(activeStartTime) < 0) {
                    time = activeStartTime;
                } else if (time != null && time.compareTo(activeEndTime) > 0) {
                    time = activeEndTime;
                }
                fixedActivities.add(time == null || time.equals(act.time())
                        ? act
                        : new AiActivityPayload(act.id(), time, act.title(), act.description(), act.category(),
                                act.durationMinutes(), act.location(), act.estimatedCost(), act.tips(),
                                act.lat(), act.lng()));
            }
            fixedDays.add(new AiDayPayload(day.day(), day.theme(), fixedActivities, day.routeWarning()));
        }
        return new AiItineraryPayload(payload.destination(), payload.durationDays(), payload.summary(), fixedDays);
    }

    /** 청크 응답들을 이어 붙이고 day 번호와 activity id를 위치 기반 전역 번호로 재부여한다. */
    private AiItineraryPayload mergeChunks(
            String destination, int durationDays, List<int[]> ranges, List<AiItineraryPayload> chunks) {
        List<AiDayPayload> mergedDays = new ArrayList<>();
        for (int c = 0; c < chunks.size(); c++) {
            int startDay = ranges.get(c)[0];
            List<AiDayPayload> chunkDays = chunks.get(c).days();
            for (int i = 0; i < chunkDays.size(); i++) {
                AiDayPayload day = chunkDays.get(i);
                int globalDay = startDay + i;
                List<AiActivityPayload> renumbered = new ArrayList<>();
                for (int a = 0; a < day.activities().size(); a++) {
                    AiActivityPayload act = day.activities().get(a);
                    renumbered.add(new AiActivityPayload(
                            "d" + globalDay + "-a" + (a + 1),
                            act.time(), act.title(), act.description(), act.category(),
                            act.durationMinutes(), act.location(), act.estimatedCost(), act.tips(),
                            act.lat(), act.lng()));
                }
                mergedDays.add(new AiDayPayload(globalDay, day.theme(), renumbered, day.routeWarning()));
            }
        }
        // 첫 청크 summary를 대표로 쓰되, 모델이 구간 일수("5박 6일"/"6일" 등)를 언급했으면 전체 일수로 정정한다.
        // 과거의 무차별 `\d+일` 치환은 "3일차" 같은 서수 표현까지 훼손했으므로, 여행 길이 표현만 좁혀서 치환한다:
        //   1) "N박 M일" → "(전체-1)박 전체일"  2) 남은 단독 "M일" → "전체일" (단, "일차/일째/일간"은 서수/기간이므로 제외)
        String summary = chunks.get(0).summary();
        if (summary != null) {
            summary = summary.replaceAll("\\d+\\s*박\\s*\\d+\\s*일", (durationDays - 1) + "박 " + durationDays + "일");
            summary = summary.replaceAll("\\d+\\s*일(?!차|째|간)", durationDays + "일");
        }
        return new AiItineraryPayload(destination, durationDays, summary, mergedDays);
    }

    private AiItineraryPayload callAndParseWithRetry(
            String userPrompt, int maxTokens, Duration timeout, int durationDays,
            String templateName, int templateVersion) {
        AiCallException lastCallException = null;
        AiParseException lastParseException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            // 초기 생성은 trip이 아직 없어 사용량 로그의 trip_id는 "-"(null)로 남긴다
            long startNanos = System.nanoTime();
            AiCallResult result;
            try {
                result = anthropicClient.complete(SYSTEM_PROMPT, userPrompt, maxTokens, timeout);
            } catch (AiCallException e) {
                aiUsageLogger.logCall(null, templateName, templateVersion, null, null,
                        elapsedMs(startNanos), false, e.code());
                lastCallException = e;
                log.warn("AI 호출 실패 (attempt {}/{})", attempt, MAX_ATTEMPTS, e);
                continue;
            }

            long elapsedMs = elapsedMs(startNanos);
            try {
                AiItineraryPayload parsed = payloadParser.parse(result.text(), durationDays);
                aiUsageLogger.logCall(null, templateName, templateVersion,
                        result.inputTokens(), result.outputTokens(), elapsedMs, true, null);
                return parsed;
            } catch (AiParseException e) {
                aiUsageLogger.logCall(null, templateName, templateVersion,
                        result.inputTokens(), result.outputTokens(), elapsedMs, false, e.code());
                lastParseException = e;
                log.warn("AI 응답 파싱/검증 실패 (attempt {}/{})", attempt, MAX_ATTEMPTS, e);
            }
        }

        if (lastParseException != null) {
            throw lastParseException;
        }
        throw lastCallException;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
