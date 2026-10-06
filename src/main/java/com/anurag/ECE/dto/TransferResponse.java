package com.anurag.ECE.dto;

import java.util.List;

public record TransferResponse(Long savedId, String origin, String destination, double synodicPeriodDays,
                               List<TransferWindowDto> windows, String vehicleName, String siteName,
                               Double payloadCapacityKg, List<String> notes) {
}
