package com.anurag.ECE.dto;

import jakarta.validation.constraints.*;

public record StageDto(
        Long id,
        Integer stageOrder,
        @NotBlank(message = "Stage name is required") @Size(max = 60) String name,
        @Positive(message = "Propellant mass must be positive") double propellantMassKg,
        @Positive(message = "Dry mass must be positive") double dryMassKg,
        @Positive(message = "Thrust must be positive") double thrustKn,
        @DecimalMin(value = "100", message = "Isp must be at least 100 s")
        @DecimalMax(value = "500", message = "Isp above 500 s is not realistic for chemical engines")
        double ispS) {
}
