package com.tripplanner.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.ai.AiCallException;
import com.tripplanner.ai.AiParseException;
import com.tripplanner.ai.ItineraryGenerationService;
import com.tripplanner.ai.PartialRegenerationViolationException;
import com.tripplanner.ai.ReorderGenerationService;
import com.tripplanner.ai.RouteWarningRuleChecker;
import com.tripplanner.ai.dto.AiActivityPayload;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiItineraryPayload;
import com.tripplanner.ai.dto.AiRouteWarningPayload;
import com.tripplanner.auth.User;
import com.tripplanner.auth.UserRepository;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import com.tripplanner.trip.dto.ActivityResponseDto;
import com.tripplanner.trip.dto.DayResponseDto;
import com.tripplanner.trip.dto.MetaDto;
import com.tripplanner.trip.dto.ReorderRequest;
import com.tripplanner.trip.dto.RouteWarningDto;
import com.tripplanner.trip.dto.TripCreateRequest;
import com.tripplanner.trip.dto.TripResponse;
import com.tripplanner.trip.dto.TripSummaryDto;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);
    private static final DateTimeFormatter TIME_OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_INPUT_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter();
    private static final LocalTime DEFAULT_ACTIVE_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_ACTIVE_END_TIME = LocalTime.of(21, 0);
    private static final int MAX_TRIP_DURATION_DAYS = 30;

    private final ItineraryGenerationService itineraryGenerationService;
    private final ReorderGenerationService reorderGenerationService;
    private final RouteWarningRuleChecker routeWarningRuleChecker;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryActivityRepository itineraryActivityRepository;
    private final TripRevisionRepository tripRevisionRepository;
    private final ObjectMapper objectMapper;
    // 자기 자신의 프록시 참조. 오케스트레이터(비트랜잭션)에서 @Transactional 영속화 메서드를 호출할 때
    // 프록시를 거쳐야 트랜잭션 경계가 적용되므로 self-injection한다(@Lazy로 순환 의존 해소). §3.1 참고.
    private final TripService self;

    public TripService(
            ItineraryGenerationService itineraryGenerationService,
            ReorderGenerationService reorderGenerationService,
            RouteWarningRuleChecker routeWarningRuleChecker,
            UserRepository userRepository,
            TripRepository tripRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryActivityRepository itineraryActivityRepository,
            TripRevisionRepository tripRevisionRepository,
            ObjectMapper objectMapper,
            @Lazy TripService self) {
        this.self = self;
        this.itineraryGenerationService = itineraryGenerationService;
        this.reorderGenerationService = reorderGenerationService;
        this.routeWarningRuleChecker = routeWarningRuleChecker;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryActivityRepository = itineraryActivityRepository;
        this.tripRevisionRepository = tripRevisionRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 최초 생성 오케스트레이션. AI 호출은 수십 초가 걸릴 수 있어 <b>트랜잭션 밖</b>에서 수행하고(§3.1: DB 커넥션
     * 장시간 점유 방지), 검증 통과 후 영속화만 별도 트랜잭션({@link #persistNewTrip})으로 처리한다.
     */
    public TripResponse createTrip(UUID userId, TripCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "end_date는 start_date보다 이후여야 합니다.");
        }
        int durationDays = (int) ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        if (durationDays > MAX_TRIP_DURATION_DAYS) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "여행 기간은 최대 30일까지 선택 가능합니다.");
        }
        // §3.2: 예산 상·하한 역전 방지(상한 NULL = 상한 없음이므로 검사 제외)
        if (request.budgetMax() != null && request.budgetMax() < request.budgetMin()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "예산 최대값은 최소값보다 크거나 같아야 합니다.");
        }

        String destination = request.destination().trim();
        String companion = request.companion().trim();
        LocalTime activeStartTime = parseTimeOrDefault(request.activeStartTime(), DEFAULT_ACTIVE_START_TIME);
        LocalTime activeEndTime = parseTimeOrDefault(request.activeEndTime(), DEFAULT_ACTIVE_END_TIME);
        // §3.3: 활동 시작 시간이 종료 시간보다 이르지 않으면 유효 범위가 없음
        if (!activeStartTime.isBefore(activeEndTime)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "활동 종료 시간은 시작 시간보다 늦어야 합니다.");
        }

        AiItineraryPayload payload;
        try {
            payload = itineraryGenerationService.generate(
                    destination,
                    durationDays,
                    request.budgetMin(),
                    request.budgetMax(),
                    companion,
                    request.preferences(),
                    request.includeNearby(),
                    activeStartTime.format(TIME_OUTPUT_FORMATTER),
                    activeEndTime.format(TIME_OUTPUT_FORMATTER));
        } catch (AiCallException | AiParseException e) {
            log.error("일정 생성 실패: destination={} durationDays={}", destination, durationDays, e);
            throw new ApiException(ErrorCode.GENERATION_FAILED, "AI 일정 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        return self.persistNewTrip(userId, request, destination, companion, durationDays,
                activeStartTime, activeEndTime, payload);
    }

    /** 생성된 payload를 Trip/Day/Activity/Revision으로 영속화한다(§3.1: AI 호출과 분리된 짧은 쓰기 트랜잭션). */
    @Transactional
    public TripResponse persistNewTrip(
            UUID userId,
            TripCreateRequest request,
            String destination,
            String companion,
            int durationDays,
            LocalTime activeStartTime,
            LocalTime activeEndTime,
            AiItineraryPayload payload) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_ERROR, "사용자를 찾을 수 없습니다."));

        OffsetDateTime now = OffsetDateTime.now();
        Trip trip = new Trip();
        trip.setUser(user);
        trip.setDestination(destination);
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setDurationDays(durationDays);
        trip.setBudgetMin(request.budgetMin());
        trip.setBudgetMax(request.budgetMax());
        trip.setCompanion(companion);
        trip.setPreferences(request.preferences().toArray(new String[0]));
        trip.setSummary(payload.summary());
        trip.setIncludeNearby(request.includeNearby());
        trip.setActiveStartTime(activeStartTime);
        trip.setActiveEndTime(activeEndTime);
        trip.setRevision(1);
        trip.setCreatedAt(now);
        trip.setUpdatedAt(now);
        trip = tripRepository.save(trip);

        for (AiDayPayload dayPayload : payload.days()) {
            ItineraryDay day = new ItineraryDay();
            day.setTrip(trip);
            day.setDayNumber(dayPayload.day());
            day.setTheme(dayPayload.theme());
            day.setRouteWarningFlagged(dayPayload.routeWarning() != null && dayPayload.routeWarning().flagged());
            day.setRouteWarningReason(dayPayload.routeWarning() != null ? dayPayload.routeWarning().reason() : null);
            day.setLastModifiedAt(null);
            day = itineraryDayRepository.save(day);

            List<AiActivityPayload> activityPayloads = dayPayload.activities();
            for (int i = 0; i < activityPayloads.size(); i++) {
                AiActivityPayload activityPayload = activityPayloads.get(i);
                ItineraryActivity activity = new ItineraryActivity();
                activity.setItineraryDay(day);
                activity.setActivityKey(
                        activityPayload.id() != null && !activityPayload.id().isBlank()
                                ? activityPayload.id()
                                : "d" + dayPayload.day() + "-a" + (i + 1));
                activity.setOrderIndex(i);
                activity.setTime(parseTime(activityPayload.time()));
                activity.setTitle(activityPayload.title());
                activity.setDescription(activityPayload.description());
                activity.setCategory(activityPayload.category());
                activity.setDurationMinutes(activityPayload.durationMinutes());
                activity.setLocation(activityPayload.location());
                activity.setEstimatedCost(activityPayload.estimatedCost());
                activity.setTips(activityPayload.tips());
                activity.setLat(activityPayload.lat());
                activity.setLng(activityPayload.lng());
                itineraryActivityRepository.save(activity);
            }
        }

        TripResponse response = buildTripResponse(trip);
        saveRevisionSnapshot(trip, response.days(), null);
        return response;
    }

    /**
     * 재조정 오케스트레이션. (1) 원본 로드+검증(읽기 트랜잭션) → (2) AI 재생성(트랜잭션 밖, §3.1) →
     * (3) 영속화(쓰기 트랜잭션)로 3단 분리해 AI 응답 대기 동안 DB 커넥션을 잡지 않는다.
     */
    public TripResponse reorder(UUID userId, UUID tripId, ReorderRequest request) {
        ReorderContext ctx = self.loadReorderContext(userId, tripId, request);

        AiDayPayload changedDayPayload;
        try {
            changedDayPayload =
                    reorderGenerationService.generate(ctx.originalPayload(), ctx.day(), request.newActivityOrder());
        } catch (AiCallException | AiParseException | PartialRegenerationViolationException e) {
            log.error("재조정 실패: tripId={} day={}", tripId, ctx.day(), e);
            throw new ApiException(ErrorCode.GENERATION_FAILED, "AI 재조정에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        boolean flagged = routeWarningRuleChecker.isInefficient(changedDayPayload.activities());
        String reason = null;
        if (flagged) {
            String aiReason = changedDayPayload.routeWarning() != null ? changedDayPayload.routeWarning().reason() : null;
            reason = (aiReason != null && !aiReason.isBlank()) ? aiReason : RouteWarningRuleChecker.DEFAULT_REASON;
        }

        return self.persistReorder(userId, tripId, ctx.day(), changedDayPayload, flagged, reason);
    }

    /** 소유권/day/순서 검증을 마친 뒤 재생성 프롬프트에 넣을 원본 전체를 완전히 materialize해 반환한다. */
    @Transactional(readOnly = true)
    public ReorderContext loadReorderContext(UUID userId, UUID tripId, ReorderRequest request) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "여행을 찾을 수 없습니다."));
        if (!trip.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 여행에 접근할 권한이 없습니다.");
        }

        int day = request.day();
        ItineraryDay targetDay = itineraryDayRepository.findByTripIdAndDayNumber(tripId, day)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "day가 유효한 범위를 벗어났습니다."));

        List<ItineraryDay> allDays = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(tripId);
        List<AiDayPayload> originalDayPayloads = new ArrayList<>();
        List<ItineraryActivity> targetDayActivities = null;
        for (ItineraryDay d : allDays) {
            List<ItineraryActivity> activities =
                    itineraryActivityRepository.findByItineraryDayIdOrderByOrderIndexAsc(d.getId());
            if (d.getId().equals(targetDay.getId())) {
                targetDayActivities = activities;
            }
            originalDayPayloads.add(toAiDayPayload(d, activities));
        }

        Set<String> existingKeys = new HashSet<>();
        for (ItineraryActivity activity : targetDayActivities) {
            existingKeys.add(activity.getActivityKey());
        }
        Set<String> requestedKeys = new HashSet<>(request.newActivityOrder());
        if (requestedKeys.size() != request.newActivityOrder().size() || !requestedKeys.equals(existingKeys)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "new_activity_order가 해당 day의 활동과 일치하지 않습니다.");
        }

        AiItineraryPayload originalPayload = new AiItineraryPayload(
                trip.getDestination(), trip.getDurationDays(), trip.getSummary(), originalDayPayloads);
        return new ReorderContext(originalPayload, day);
    }

    /** 검증된 changedDay 결과만 대상 day에 반영한다(다른 날짜는 손대지 않음). 짧은 쓰기 트랜잭션. */
    @Transactional
    public TripResponse persistReorder(
            UUID userId, UUID tripId, int day, AiDayPayload changedDayPayload, boolean flagged, String reason) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "여행을 찾을 수 없습니다."));
        if (!trip.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 여행에 접근할 권한이 없습니다.");
        }
        ItineraryDay targetDay = itineraryDayRepository.findByTripIdAndDayNumber(tripId, day)
                .orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_ERROR, "day가 유효한 범위를 벗어났습니다."));

        OffsetDateTime now = OffsetDateTime.now();

        itineraryActivityRepository.deleteByItineraryDayId(targetDay.getId());
        // Hibernate는 같은 flush에서 삭제보다 삽입을 먼저 실행하므로, 명시적으로 flush해
        // (itinerary_day_id, order_index) 유니크 제약과 충돌하지 않게 한다.
        itineraryActivityRepository.flush();
        List<AiActivityPayload> newActivities = changedDayPayload.activities();
        for (int i = 0; i < newActivities.size(); i++) {
            AiActivityPayload activityPayload = newActivities.get(i);
            ItineraryActivity activity = new ItineraryActivity();
            activity.setItineraryDay(targetDay);
            activity.setActivityKey(activityPayload.id());
            activity.setOrderIndex(i);
            activity.setTime(parseTime(activityPayload.time()));
            activity.setTitle(activityPayload.title());
            activity.setDescription(activityPayload.description());
            activity.setCategory(activityPayload.category());
            activity.setDurationMinutes(activityPayload.durationMinutes());
            activity.setLocation(activityPayload.location());
            activity.setEstimatedCost(activityPayload.estimatedCost());
            activity.setTips(activityPayload.tips());
            activity.setLat(activityPayload.lat());
            activity.setLng(activityPayload.lng());
            itineraryActivityRepository.save(activity);
        }

        targetDay.setTheme(changedDayPayload.theme());
        targetDay.setRouteWarningFlagged(flagged);
        targetDay.setRouteWarningReason(reason);
        targetDay.setLastModifiedAt(now);
        itineraryDayRepository.save(targetDay);

        trip.setRevision(trip.getRevision() + 1);
        trip.setUpdatedAt(now);
        trip = tripRepository.save(trip);

        TripResponse response = buildTripResponse(trip);
        saveRevisionSnapshot(trip, response.days(), day);
        return response;
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(UUID userId, UUID tripId, boolean isAdmin) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "여행을 찾을 수 없습니다."));

        if (!isAdmin && !trip.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 여행에 접근할 권한이 없습니다.");
        }

        return buildTripResponse(trip);
    }

    @Transactional
    public void deleteTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "여행을 찾을 수 없습니다."));
        if (!trip.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 여행에 접근할 권한이 없습니다.");
        }
        // days/activities/revisions는 FK ON DELETE CASCADE로 함께 삭제된다
        tripRepository.delete(trip);
    }

    @Transactional(readOnly = true)
    public List<TripSummaryDto> listTrips(UUID userId) {
        return tripRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(trip -> new TripSummaryDto(
                        trip.getId(),
                        trip.getDestination(),
                        trip.getSummary(),
                        trip.getStartDate(),
                        trip.getEndDate(),
                        trip.getDurationDays(),
                        List.of(trip.getPreferences())))
                .toList();
    }

    private TripResponse buildTripResponse(Trip trip) {
        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(trip.getId());
        List<DayResponseDto> dayResponses = new ArrayList<>();
        for (ItineraryDay day : days) {
            List<ItineraryActivity> activities =
                    itineraryActivityRepository.findByItineraryDayIdOrderByOrderIndexAsc(day.getId());
            List<ActivityResponseDto> activityResponses = activities.stream().map(this::mapActivity).toList();

            boolean lastModified = day.getLastModifiedAt() != null && day.getLastModifiedAt().isEqual(trip.getUpdatedAt());
            dayResponses.add(new DayResponseDto(
                    day.getDayNumber(),
                    day.getTheme(),
                    activityResponses,
                    lastModified,
                    new RouteWarningDto(day.isRouteWarningFlagged(), day.getRouteWarningReason())));
        }

        return new TripResponse(
                trip.getId(),
                trip.getDestination(),
                trip.getDurationDays(),
                trip.getSummary(),
                dayResponses,
                new MetaDto(trip.getUpdatedAt(), trip.getRevision()));
    }

    private AiDayPayload toAiDayPayload(ItineraryDay day, List<ItineraryActivity> activities) {
        List<AiActivityPayload> activityPayloads = activities.stream().map(this::toAiActivityPayload).toList();
        AiRouteWarningPayload routeWarning =
                new AiRouteWarningPayload(day.isRouteWarningFlagged(), day.getRouteWarningReason());
        return new AiDayPayload(day.getDayNumber(), day.getTheme(), activityPayloads, routeWarning);
    }

    private AiActivityPayload toAiActivityPayload(ItineraryActivity activity) {
        return new AiActivityPayload(
                activity.getActivityKey(),
                activity.getTime() != null ? activity.getTime().format(TIME_OUTPUT_FORMATTER) : null,
                activity.getTitle(),
                activity.getDescription(),
                activity.getCategory(),
                activity.getDurationMinutes(),
                activity.getLocation(),
                activity.getEstimatedCost(),
                activity.getTips(),
                activity.getLat(),
                activity.getLng());
    }

    private ActivityResponseDto mapActivity(ItineraryActivity activity) {
        return new ActivityResponseDto(
                activity.getActivityKey(),
                activity.getTime() != null ? activity.getTime().format(TIME_OUTPUT_FORMATTER) : null,
                activity.getTitle(),
                activity.getDescription(),
                activity.getCategory(),
                activity.getDurationMinutes(),
                activity.getLocation(),
                activity.getEstimatedCost(),
                activity.getTips(),
                activity.getLat(),
                activity.getLng());
    }

    private void saveRevisionSnapshot(Trip trip, List<DayResponseDto> dayResponses, Integer changedDayNumber) {
        try {
            TripRevision revision = new TripRevision();
            revision.setTrip(trip);
            revision.setRevisionNumber(trip.getRevision());
            revision.setChangedDayNumber(changedDayNumber);
            revision.setDaysSnapshot(objectMapper.writeValueAsString(dayResponses));
            revision.setCreatedAt(OffsetDateTime.now());
            tripRevisionRepository.save(revision);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "여행 이력 저장에 실패했습니다.");
        }
    }

    private LocalTime parseTimeOrDefault(String raw, LocalTime fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return LocalTime.parse(raw.trim(), TIME_OUTPUT_FORMATTER);
    }

    private LocalTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(raw.trim(), TIME_INPUT_FORMATTER);
        } catch (Exception e) {
            log.warn("activity time 파싱 실패, null로 대체: raw={}", raw);
            return null;
        }
    }

    /** 재조정 읽기 단계 산출물: 트랜잭션 밖 AI 호출에 필요한 원본 전체와 대상 day. */
    public record ReorderContext(AiItineraryPayload originalPayload, int day) {
    }
}
