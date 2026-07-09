package com.tripplanner.trip;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, UUID> {
}
