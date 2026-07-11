package com.tripplanner.trip;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, UUID> {

    List<ItineraryDay> findByTripIdOrderByDayNumberAsc(UUID tripId);

    Optional<ItineraryDay> findByTripIdAndDayNumber(UUID tripId, int dayNumber);

    @Query("SELECT d FROM ItineraryDay d JOIN FETCH d.trip WHERE d.routeWarningFlagged = true")
    List<ItineraryDay> findAllFlaggedWithTrip();

    long countByRouteWarningFlaggedTrue();
}
