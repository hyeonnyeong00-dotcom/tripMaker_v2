package com.tripplanner.trip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripplanner.ai.AiCallException;
import com.tripplanner.ai.AiParseException;
import com.tripplanner.ai.ItineraryGenerationService;
import com.tripplanner.ai.dto.AiActivityPayload;
import com.tripplanner.ai.dto.AiDayPayload;
import com.tripplanner.ai.dto.AiItineraryPayload;
import com.tripplanner.auth.User;
import com.tripplanner.auth.UserRepository;
import com.tripplanner.common.ApiException;
import com.tripplanner.common.ErrorCode;
import com.tripplanner.trip.dto.ActivityResponseDto;
import com.tripplanner.trip.dto.DayResponseDto;
import com.tripplanner.trip.dto.MetaDto;
import com.tripplanner.trip.dto.RouteWarningDto;
import com.tripplanner.trip.dto.TripCreateRequest;
import com.tripplanner.trip.dto.TripResponse;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ItineraryGenerationService itineraryGenerationService;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final ItineraryDayRepository itineraryDayRepository;
    private final ItineraryActivityRepository itineraryActivityRepository;
    private final TripRevisionRepository tripRevisionRepository;
    private final ObjectMapper objectMapper;

    public TripService(
            ItineraryGenerationService itineraryGenerationService,
            UserRepository userRepository,
            TripRepository tripRepository,
            ItineraryDayRepository itineraryDayRepository,
            ItineraryActivityRepository itineraryActivityRepository,
            TripRevisionRepository tripRevisionRepository,
            ObjectMapper objectMapper) {
        this.itineraryGenerationService = itineraryGenerationService;
        this.userRepository = userRepository;
        this.tripRepository = tripRepository;
        this.itineraryDayRepository = itineraryDayRepository;
        this.itineraryActivityRepository = itineraryActivityRepository;
        this.tripRevisionRepository = tripRevisionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TripResponse createTrip(UUID userId, TripCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "end_date는 start_date보다 이후여야 합니다.");
        }
        int durationDays = (int) ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_ERROR, "사용자를 찾을 수 없습니다."));

        String destination = request.destination().trim();
        AiItineraryPayload payload;
        try {
            payload = itineraryGenerationService.generate(
                    destination, durationDays, request.budgetLevel(), request.preferences(), request.includeNearby());
        } catch (AiCallException | AiParseException e) {
            log.error("일정 생성 실패: destination={} durationDays={}", destination, durationDays, e);
            throw new ApiException(ErrorCode.GENERATION_FAILED, "AI 일정 생성에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        Trip trip = new Trip();
        trip.setUser(user);
        trip.setDestination(destination);
        trip.setStartDate(request.startDate());
        trip.setEndDate(request.endDate());
        trip.setDurationDays(durationDays);
        trip.setBudgetLevel(request.budgetLevel().trim());
        trip.setPreferences(request.preferences().toArray(new String[0]));
        trip.setSummary(payload.summary());
        trip.setIncludeNearby(request.includeNearby());
        trip.setRevision(1);
        trip.setCreatedAt(now);
        trip.setUpdatedAt(now);
        trip = tripRepository.save(trip);

        List<DayResponseDto> dayResponses = new ArrayList<>();
        for (AiDayPayload dayPayload : payload.days()) {
            ItineraryDay day = new ItineraryDay();
            day.setTrip(trip);
            day.setDayNumber(dayPayload.day());
            day.setTheme(dayPayload.theme());
            day.setRouteWarningFlagged(dayPayload.routeWarning() != null && dayPayload.routeWarning().flagged());
            day.setRouteWarningReason(dayPayload.routeWarning() != null ? dayPayload.routeWarning().reason() : null);
            day.setLastModifiedAt(null);
            day = itineraryDayRepository.save(day);

            List<ActivityResponseDto> activityResponses = new ArrayList<>();
            List<AiActivityPayload> activityPayloads = dayPayload.activities();
            for (int i = 0; i < activityPayloads.size(); i++) {
                AiActivityPayload activityPayload = activityPayloads.get(i);
                LocalTime time = parseTime(activityPayload.time());

                ItineraryActivity activity = new ItineraryActivity();
                activity.setItineraryDay(day);
                activity.setActivityKey(
                        activityPayload.id() != null && !activityPayload.id().isBlank()
                                ? activityPayload.id()
                                : "d" + dayPayload.day() + "-a" + (i + 1));
                activity.setOrderIndex(i);
                activity.setTime(time);
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

                activityResponses.add(mapActivity(activity));
            }

            dayResponses.add(new DayResponseDto(
                    dayPayload.day(),
                    dayPayload.theme(),
                    activityResponses,
                    false,
                    new RouteWarningDto(day.isRouteWarningFlagged(), day.getRouteWarningReason())));
        }

        saveRevisionSnapshot(trip, dayResponses);

        return new TripResponse(trip.getId(), destination, durationDays, payload.summary(), dayResponses, new MetaDto(now, 1));
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(UUID userId, UUID tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "여행을 찾을 수 없습니다."));

        if (!trip.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "이 여행에 접근할 권한이 없습니다.");
        }

        List<ItineraryDay> days = itineraryDayRepository.findByTripIdOrderByDayNumberAsc(tripId);
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

    private void saveRevisionSnapshot(Trip trip, List<DayResponseDto> dayResponses) {
        try {
            TripRevision revision = new TripRevision();
            revision.setTrip(trip);
            revision.setRevisionNumber(1);
            revision.setChangedDayNumber(null);
            revision.setDaysSnapshot(objectMapper.writeValueAsString(dayResponses));
            revision.setCreatedAt(OffsetDateTime.now());
            tripRevisionRepository.save(revision);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.STORAGE_ERROR, "여행 이력 저장에 실패했습니다.");
        }
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
}
