package com.anurag.ECE.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Parameters for a shockFLOW aero-refine sweep. All fields are optional; the
 * service fills in sensible defaults (fineness 3.0, Mach 0.3-5.0, 24 points).
 */
public record AeroRefineRequest(
        @DecimalMin(value = "1.0", message = "Fineness ratio must be at least 1.0")
        @DecimalMax(value = "20.0", message = "Fineness ratio must be at most 20.0") Double noseFinenessRatio,
        @DecimalMin(value = "0.1", message = "Mach range must start at 0.1 or above") Double machMin,
        @DecimalMax(value = "8.0", message = "Mach range must end at 8.0 or below") Double machMax,
        @Min(value = 4, message = "At least 4 points are required")
        @Max(value = 60, message = "At most 60 points are supported") Integer points) {
}
