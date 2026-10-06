package com.anurag.ECE.dto;

import com.anurag.ECE.entity.OrbitType;
import jakarta.validation.constraints.*;

import java.time.Instant;

/**
 * Launch planning request. For GTO, {@code altitudeKm} is the altitude of the parking orbit.
 * If {@code inclinationDeg} is omitted for LEO/GTO, the site latitude (due-east launch) is used.
 */
public record MissionRequest(
        @NotBlank(message = "Mission name is required") @Size(max = 100) String missionName,
        @NotNull(message = "Select a launch vehicle") Long vehicleId,
        @NotNull(message = "Select a launch site") Long siteId,
        @NotNull(message = "Select an orbit type") OrbitType orbitType,
        @NotNull @DecimalMin(value = "150", message = "Altitude must be at least 150 km")
        @DecimalMax(value = "2000", message = "Altitude must be at most 2000 km") Double altitudeKm,
        @DecimalMin(value = "0", message = "Inclination must be between 0 and 180")
        @DecimalMax(value = "180", message = "Inclination must be between 0 and 180") Double inclinationDeg,
        @NotNull @PositiveOrZero(message = "Payload cannot be negative") Double payloadKg,
        @DecimalMin("0") @DecimalMax("360") Double targetRaanDeg,
        Instant earliestLaunchUtc,
        Long operatorId,
        boolean save) {
}
