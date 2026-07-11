package com.tripplanner.admin;

import com.tripplanner.admin.dto.DestinationStatDto;
import com.tripplanner.admin.dto.DestinationStatsResponse;
import com.tripplanner.admin.dto.FlaggedTripDto;
import com.tripplanner.admin.dto.FlaggedTripsResponse;
import com.tripplanner.trip.ItineraryDay;
import com.tripplanner.trip.ItineraryDayRepository;
import com.tripplanner.trip.TripRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMonitoringService {

    private static final int DESTINATION_STATS_PERIOD_DAYS = 30;

    private final ItineraryDayRepository itineraryDayRepository;
    private final TripRepository tripRepository;

    public AdminMonitoringService(ItineraryDayRepository itineraryDayRepository, TripRepository tripRepository) {
        this.itineraryDayRepository = itineraryDayRepository;
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public FlaggedTripsResponse flaggedTrips() {
        List<ItineraryDay> flaggedDays = itineraryDayRepository.findAllFlaggedWithTrip();

        List<FlaggedTripDto> dtos = flaggedDays.stream()
                .map(day -> new FlaggedTripDto(
                        day.getTrip().getId(),
                        day.getTrip().getDestination(),
                        day.getDayNumber(),
                        day.getTheme(),
                        day.getRouteWarningReason(),
                        flaggedAt(day)))
                .sorted(Comparator.comparing(FlaggedTripDto::flaggedAt).reversed())
                .toList();

        long totalDays = itineraryDayRepository.count();
        long flaggedCount = itineraryDayRepository.countByRouteWarningFlaggedTrue();
        double ratio = totalDays == 0 ? 0.0 : (double) flaggedCount / totalDays;

        return new FlaggedTripsResponse(dtos, flaggedCount, totalDays, ratio);
    }

    @Transactional(readOnly = true)
    public DestinationStatsResponse destinationStats() {
        OffsetDateTime since = OffsetDateTime.now().minusDays(DESTINATION_STATS_PERIOD_DAYS);

        List<DestinationStatDto> destinations = tripRepository.aggregateDestinationCounts(since).stream()
                .map(row -> new DestinationStatDto((String) row[0], (Long) row[1]))
                .toList();

        long totalTrips = tripRepository.countByCreatedAtAfter(since);

        return new DestinationStatsResponse(destinations, totalTrips, DESTINATION_STATS_PERIOD_DAYS);
    }

    /** lastModifiedAt은 재조정으로 flagged된 경우에만 채워지므로, 최초 생성 시 flagged된 경우 trip 생성 시각으로 대체한다. */
    private OffsetDateTime flaggedAt(ItineraryDay day) {
        return day.getLastModifiedAt() != null ? day.getLastModifiedAt() : day.getTrip().getCreatedAt();
    }
}
