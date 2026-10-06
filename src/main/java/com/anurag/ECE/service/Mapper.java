package com.anurag.ECE.service;

import com.anurag.ECE.dto.DragPointDto;
import com.anurag.ECE.dto.SiteDto;
import com.anurag.ECE.dto.StageDto;
import com.anurag.ECE.dto.VehicleDto;
import com.anurag.ECE.entity.LaunchSite;
import com.anurag.ECE.entity.LaunchVehicle;
import com.anurag.ECE.entity.Stage;
import com.anurag.ECE.physics.StageSpec;
import com.anurag.ECE.physics.VehicleSpec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/** Converts between entities, DTOs and physics specs. */
public final class Mapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    private Mapper() {
    }

    public static VehicleDto toDto(LaunchVehicle v) {
        List<StageDto> stages = v.getStages().stream()
                .map(s -> new StageDto(s.getId(), s.getStageOrder(), s.getName(), s.getPropellantMassKg(),
                        s.getDryMassKg(), s.getThrustKn(), s.getIspS()))
                .toList();
        VehicleSpec spec = toSpec(v);
        return new VehicleDto(v.getId(), v.getName(), v.getManufacturer(), v.getCountry(), v.getDiameterM(),
                v.getDragCoefficient(), stages, spec.liftoffMass(0), spec.idealDeltaV(0),
                v.getAeroSource(), dragCurveDtos(v.getDragCurveJson()));
    }

    public static void copy(VehicleDto dto, LaunchVehicle v) {
        v.setName(dto.name().trim());
        v.setManufacturer(dto.manufacturer());
        v.setCountry(dto.country());
        v.setDiameterM(dto.diameterM());
        v.setDragCoefficient(dto.dragCoefficient());
        v.replaceStages(dto.stages().stream()
                .map(s -> new Stage(s.name().trim(), s.propellantMassKg(), s.dryMassKg(), s.thrustKn(), s.ispS()))
                .toList());
        // aeroSource / dragCurveJson are intentionally left untouched here: they are
        // only ever set by VehicleService.refineAero / resetAero, so editing the
        // rest of a vehicle's data never silently discards a fetched drag curve.
    }

    public static VehicleSpec toSpec(LaunchVehicle v) {
        List<StageSpec> stages = v.getStages().stream()
                .map(s -> new StageSpec(s.getName(), s.getPropellantMassKg(), s.getDryMassKg(),
                        s.getThrustKn() * 1000, s.getIspS()))
                .toList();
        double[][] curve = dragCurveArrays(v.getDragCurveJson());
        if (curve == null) {
            return new VehicleSpec(v.getName(), stages, v.getDiameterM(), v.getDragCoefficient());
        }
        return new VehicleSpec(v.getName(), stages, v.getDiameterM(), v.getDragCoefficient(), curve[0], curve[1]);
    }

    /** Serializes an {@link com.anurag.ECE.aero.AeroCurveResult} to the JSON stored on the vehicle. */
    public static String dragCurveToJson(double[] mach, double[] cd) {
        try {
            List<Object[]> points = new ArrayList<>();
            for (int i = 0; i < mach.length; i++) points.add(new Object[]{mach[i], cd[i]});
            return JSON.writeValueAsString(points);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize drag curve", e);
        }
    }

    private static List<DragPointDto> dragCurveDtos(String json) {
        double[][] arrays = dragCurveArrays(json);
        if (arrays == null) return List.of();
        List<DragPointDto> points = new ArrayList<>();
        for (int i = 0; i < arrays[0].length; i++) points.add(new DragPointDto(arrays[0][i], arrays[1][i]));
        return points;
    }

    private static double[][] dragCurveArrays(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            JsonNode root = JSON.readTree(json);
            double[] mach = new double[root.size()];
            double[] cd = new double[root.size()];
            for (int i = 0; i < root.size(); i++) {
                JsonNode pair = root.get(i);
                mach[i] = pair.get(0).asDouble();
                cd[i] = pair.get(1).asDouble();
            }
            return new double[][]{mach, cd};
        } catch (Exception e) {
            return null;
        }
    }

    public static SiteDto toDto(LaunchSite s) {
        return new SiteDto(s.getId(), s.getName(), s.getCountry(), s.getLatitudeDeg(), s.getLongitudeDeg(),
                s.getMinAzimuthDeg(), s.getMaxAzimuthDeg());
    }

    public static void copy(SiteDto dto, LaunchSite s) {
        s.setName(dto.name().trim());
        s.setCountry(dto.country());
        s.setLatitudeDeg(dto.latitudeDeg());
        s.setLongitudeDeg(dto.longitudeDeg());
        s.setMinAzimuthDeg(dto.minAzimuthDeg());
        s.setMaxAzimuthDeg(dto.maxAzimuthDeg());
    }
}
