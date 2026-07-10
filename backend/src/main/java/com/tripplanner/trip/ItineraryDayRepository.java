package com.tripplanner.trip;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, UUID> {

    List<ItineraryDay> findByTripIdOrderByDayNumberAsc(UUID tripId);
}
