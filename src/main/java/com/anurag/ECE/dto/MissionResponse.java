package com.anurag.ECE.dto;

import com.anurag.ECE.entity.OrbitType;
import com.anurag.ECE.physics.TrajectoryPoint;

import java.time.Instant;
import java.util.List;

public record MissionResponse(
        Long id,
        String missionName,
        Instant createdAt,
        String plannedBy,
        Long plannedById,
        String vehicleName,
        String siteName,
        double siteLatitudeDeg,
        double siteLongitudeDeg,
        OrbitType orbitType,
        double targetAltitudeKm,
        double inclinationDeg,
        double payloadKg,
        boolean feasible,
        String failureReason,
        double launchAzimuthDeg,
        double pitchKickDeg,
        double finalPitchDeg,
        double insertionTimeS,
        double insertionAltitudeKm,
        double gravityLossMs,
        double dragLossMs,
        double steeringLossMs,
        double ascentDvMs,
        double insertionDvMs,
        double postInsertionDvMs,
        double totalDvMs,
        double dvMarginMs,
        double maxPayloadKg,
        double maxQkPa,
        Instant nextWindowUtc,
        List<String> notes,
        List<TrajectoryPoint> trajectory) {
}
