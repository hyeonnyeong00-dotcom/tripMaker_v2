package com.tripplanner.trip;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripRepository extends JpaRepository<Trip, UUID> {

    List<Trip> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    long countByCreatedAtAfter(OffsetDateTime since);

    @Query("SELECT t.destination, COUNT(t) FROM Trip t WHERE t.createdAt >= :since GROUP BY t.destination ORDER BY COUNT(t) DESC")
    List<Object[]> aggregateDestinationCounts(@Param("since") OffsetDateTime since);
}
