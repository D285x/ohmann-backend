package com.anurag.ECE.dto;

import jakarta.validation.constraints.*;

public record SiteDto(
        Long id,
        @NotBlank(message = "Site name is required") @Size(max = 100) String name,
        @Size(max = 60) String country,
        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90") double latitudeDeg,
        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180") double longitudeDeg,
        @DecimalMin("0") @DecimalMax("360") double minAzimuthDeg,
        @DecimalMin("0") @DecimalMax("360") double maxAzimuthDeg) {
}
