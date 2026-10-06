package com.anurag.ECE.dto;

import com.anurag.ECE.entity.BodyType;
import jakarta.validation.constraints.*;

public record BodyDto(
        Long id,
        @NotBlank(message = "Name is required") @Size(max = 40) String name,
        @NotNull(message = "Select a body type") BodyType bodyType,
        @DecimalMin(value = "0.05", message = "Orbit radius must be at least 0.05 AU")
        @DecimalMax(value = "200", message = "Orbit radius must be at most 200 AU") double semiMajorAxisAu,
        @DecimalMin(value = "0", message = "Mean longitude must be 0 to 360")
        @DecimalMax(value = "360", message = "Mean longitude must be 0 to 360") double meanLongitudeJ2000Deg,
        @Positive(message = "GM must be positive") double gmKm3s2,
        @Positive(message = "Radius must be positive") double radiusKm,
        // read-only, computed by the server
        Double orbitalPeriodDays,
        Double surfaceGravity,
        Double currentMeanLongitudeDeg) {
}
