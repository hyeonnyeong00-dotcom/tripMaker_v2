package com.tripplanner.trip;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryActivityRepository extends JpaRepository<ItineraryActivity, UUID> {

    List<ItineraryActivity> findByItineraryDayIdOrderByOrderIndexAsc(UUID itineraryDayId);
}
