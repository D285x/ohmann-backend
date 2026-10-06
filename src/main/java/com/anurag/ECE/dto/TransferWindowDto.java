package com.anurag.ECE.dto;

import java.time.Instant;

public record TransferWindowDto(Instant departureUtc, Instant arrivalUtc, double timeOfFlightDays,
                                double phaseAngleDeg, double c3Km2s2, double vInfDepartureMs,
                                double vInfArrivalMs, double departureDvMs, double arrivalDvMs,
                                double totalDvMs) {
}
