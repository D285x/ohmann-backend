package com.anurag.ECE.dto;

import java.util.List;
import java.util.Map;

public record StatsDto(long vehicles, long sites, long bodies, long operators, long missions,
                       long feasibleMissions, long transfers,
                       Map<String, Long> missionsByOrbit, List<VehicleStat> vehicleStats) {

    public record VehicleStat(String vehicle, long missions, double avgMaxPayloadKg, double bestMarginMs) {
    }
}
