package com.tripplanner.trip;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
