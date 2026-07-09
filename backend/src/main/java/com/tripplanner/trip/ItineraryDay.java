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

@Entity
@Table(name = "itinerary_days")
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column
    private String theme;

    @Column(name = "route_warning_flagged", nullable = false)
    private boolean routeWarningFlagged;

    @Column(name = "route_warning_reason")
    private String routeWarningReason;

    @Column(name = "last_modified_at")
    private OffsetDateTime lastModifiedAt;

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

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public boolean isRouteWarningFlagged() {
        return routeWarningFlagged;
    }

    public void setRouteWarningFlagged(boolean routeWarningFlagged) {
        this.routeWarningFlagged = routeWarningFlagged;
    }

    public String getRouteWarningReason() {
        return routeWarningReason;
    }

    public void setRouteWarningReason(String routeWarningReason) {
        this.routeWarningReason = routeWarningReason;
    }

    public OffsetDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(OffsetDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }
}
