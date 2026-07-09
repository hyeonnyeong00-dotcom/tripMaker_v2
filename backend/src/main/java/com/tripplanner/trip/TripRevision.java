package com.tripplanner.trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "trip_revisions")
public class TripRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "changed_day_number")
    private Integer changedDayNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "days_snapshot", nullable = false)
    private String daysSnapshot;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public void setRevisionNumber(int revisionNumber) {
        this.revisionNumber = revisionNumber;
    }

    public Integer getChangedDayNumber() {
        return changedDayNumber;
    }

    public void setChangedDayNumber(Integer changedDayNumber) {
        this.changedDayNumber = changedDayNumber;
    }

    public String getDaysSnapshot() {
        return daysSnapshot;
    }

    public void setDaysSnapshot(String daysSnapshot) {
        this.daysSnapshot = daysSnapshot;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
