package com.anurag.ECE.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record VehicleDto(
        Long id,
        @NotBlank(message = "Vehicle name is required") @Size(max = 80) String name,
        @Size(max = 80) String manufacturer,
        @Size(max = 60) String country,
        @Positive(message = "Diameter must be positive") @DecimalMax("20") double diameterM,
        @DecimalMin(value = "0.05", message = "Drag coefficient must be at least 0.05")
        @DecimalMax(value = "2.0", message = "Drag coefficient must be at most 2.0") double dragCoefficient,
        @NotEmpty(message = "At least one stage is required")
        @Size(max = 6, message = "At most 6 stages are supported") @Valid List<StageDto> stages,
        // read-only, computed by the server
        Double liftoffMassKg,
        Double idealDeltaVMs,
        // read-only, set by POST /{id}/aero/refine and DELETE /{id}/aero
        String aeroSource,
        List<DragPointDto> dragCurve) {
}
