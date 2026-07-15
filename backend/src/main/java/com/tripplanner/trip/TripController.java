package com.tripplanner.trip;

import com.tripplanner.trip.dto.ReorderRequest;
import com.tripplanner.trip.dto.TripCreateRequest;
import com.tripplanner.trip.dto.TripResponse;
import com.tripplanner.trip.dto.TripSummaryDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trips", description = "여행 일정 최초 생성/목록/상세/재조정(부분 재생성)/삭제")
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TripResponse> create(
            Authentication authentication, @Valid @RequestBody TripCreateRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(tripService.createTrip(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<TripSummaryDto>> list(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tripService.listTrips(userId));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripResponse> get(Authentication authentication, @PathVariable UUID tripId) {
        UUID userId = UUID.fromString(authentication.getName());
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        return ResponseEntity.ok(tripService.getTrip(userId, tripId, isAdmin));
    }

    @DeleteMapping("/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable UUID tripId) {
        UUID userId = UUID.fromString(authentication.getName());
        tripService.deleteTrip(userId, tripId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{tripId}/reorder")
    public ResponseEntity<TripResponse> reorder(
            Authentication authentication, @PathVariable UUID tripId, @Valid @RequestBody ReorderRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(tripService.reorder(userId, tripId, request));
    }
}
