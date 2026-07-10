package com.tripplanner.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.ai.AiResponseCache;
import com.tripplanner.ai.AiResponseCacheRepository;
import com.tripplanner.ai.dto.AiItineraryPayload;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CLAUDE.md 5.3: TTL 30일 lazy expiry(조회 시 created_at 초과분은 미스 처리 후 새 응답 upsert),
 * 캐시 조회 실패 시 예외를 삼키고 AI 직접 호출로 폴백한다.
 */
@Service
public class AiResponseCacheService {

    private static final Logger log = LoggerFactory.getLogger(AiResponseCacheService.class);
    private static final int TTL_DAYS = 30;

    private final AiResponseCacheRepository repository;
    private final ObjectMapper objectMapper;

    public AiResponseCacheService(AiResponseCacheRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<AiItineraryPayload> lookup(String cacheKey) {
        try {
            Optional<AiResponseCache> cached = repository.findByCacheKey(cacheKey);
            if (cached.isEmpty()) {
                return Optional.empty();
            }

            AiResponseCache row = cached.get();
            if (row.getCreatedAt().isBefore(OffsetDateTime.now().minusDays(TTL_DAYS))) {
                log.info("cache expired(30d lazy) cacheKey={}", cacheKey);
                return Optional.empty();
            }

            return Optional.of(objectMapper.readValue(row.getResponseJson(), AiItineraryPayload.class));
        } catch (Exception e) {
            log.warn("cache lookup 실패, AI 직접 호출로 폴백. cacheKey={}", cacheKey, e);
            return Optional.empty();
        }
    }

    @Transactional
    public void upsert(String cacheKey, Map<String, Object> requestParams, AiItineraryPayload payload) {
        try {
            String requestParamsJson = objectMapper.writeValueAsString(requestParams);
            String responseJson = objectMapper.writeValueAsString(payload);

            AiResponseCache row = repository.findByCacheKey(cacheKey).orElseGet(AiResponseCache::new);
            row.setCacheKey(cacheKey);
            row.setRequestParams(requestParamsJson);
            row.setResponseJson(responseJson);
            row.setCreatedAt(OffsetDateTime.now());
            repository.save(row);
        } catch (JsonProcessingException e) {
            log.warn("cache upsert 실패(직렬화 오류) cacheKey={}", cacheKey, e);
        } catch (Exception e) {
            log.warn("cache upsert 실패 cacheKey={}", cacheKey, e);
        }
    }
}
