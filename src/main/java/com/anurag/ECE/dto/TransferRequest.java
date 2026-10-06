package com.anurag.ECE.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record TransferRequest(
        @NotNull(message = "Select an origin") Long originId,
        @NotNull(message = "Select a destination") Long destinationId,
        Instant earliestDepartureUtc,
        @Min(value = 1, message = "At least 1 window") @Max(value = 10, message = "At most 10 windows") Integer windowCount,
        @NotNull @DecimalMin(value = "100", message = "Parking orbit must be at least 100 km") @DecimalMax("100000") Double parkingAltitudeKm,
        /** Capture orbit altitude at the destination; null means flyby (no capture burn). */
        @DecimalMin(value = "10", message = "Capture orbit must be at least 10 km") @DecimalMax("1000000") Double captureAltitudeKm,
        Long vehicleId,
        Long siteId,
        Long operatorId,
        boolean save) {
}
