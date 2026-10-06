package com.anurag.ECE.dto;

import java.time.Instant;

public record TransferSummary(Long id, Instant createdAt, String origin, String destination,
                              Instant departureUtc, Instant arrivalUtc, double timeOfFlightDays,
                              double c3Km2s2, double departureDvMs, double arrivalDvMs, double totalDvMs,
                              String vehicleName, Double payloadCapacityKg, String plannedBy) {
}
