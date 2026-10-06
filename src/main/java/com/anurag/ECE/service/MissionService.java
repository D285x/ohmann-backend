package com.anurag.ECE.service;

import com.anurag.ECE.dto.MissionRequest;
import com.anurag.ECE.dto.MissionResponse;
import com.anurag.ECE.entity.LaunchSite;
import com.anurag.ECE.entity.LaunchVehicle;
import com.anurag.ECE.entity.MissionPlan;
import com.anurag.ECE.exception.ResourceNotFoundException;
import com.anurag.ECE.physics.*;
import com.anurag.ECE.repository.MissionPlanRepository;
import com.anurag.ECE.util.CsvExporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class MissionService {

    private final MissionPlanRepository repo;
    private final VehicleService vehicles;
    private final SiteService sites;
    private final AscentOptimizer optimizer;
    private final UserService users;
    private final Path exportDir;

    public MissionService(MissionPlanRepository repo, VehicleService vehicles, SiteService sites,
                          AscentOptimizer optimizer, UserService users,
                          @Value("${ohmann.export-dir}") String exportDir) {
        this.users = users;
        this.repo = repo;
        this.vehicles = vehicles;
        this.sites = sites;
        this.optimizer = optimizer;
        this.exportDir = Path.of(exportDir);
    }

    @Transactional
    public MissionResponse plan(MissionRequest req) {
        LaunchVehicle vehicle = vehicles.getEntity(req.vehicleId());
        LaunchSite site = sites.getEntity(req.siteId());
        VehicleSpec spec = Mapper.toSpec(vehicle);
        LaunchGeometry geo = LaunchGeometry.of(site, req.orbitType(), req.altitudeKm(), req.inclinationDeg());
        List<String> notes = new ArrayList<>(geo.notes());
        double rot = geo.azimuth().rotationAssistMs();
        double extra = geo.postInsertionDv();

        AscentResult best = optimizer.optimize(spec, req.payloadKg(), geo.insertionAltM(), rot, extra);
        AscentOptimizer.CapacityResult cap = optimizer.payloadCapacity(spec, geo.insertionAltM(), rot, extra,
                best.guidance());

        // re-run the best solution with trajectory recording for the charts
        AscentResult shown = new AscentSimulator(spec, req.payloadKg(), geo.insertionAltM(), rot)
                .simulate(best.guidance(), true);

        boolean feasible = shown.success() && shown.margin(extra) >= 0;
        String failure = null;
        if (!shown.success()) {
            failure = shown.failureReason();
        } else if (!feasible) {
            failure = String.format("Insufficient delta-v: short by %.0f m/s. Maximum payload is %.0f kg",
                    -shown.margin(extra), cap.payloadKg());
        }

        Instant window = null;
        if (req.targetRaanDeg() != null) {
            if (geo.azimuth().inPlane()) {
                Instant from = req.earliestLaunchUtc() != null ? req.earliestLaunchUtc() : Instant.now();
                window = OrbitalMechanics.nextLaunchWindow(from, site.getLatitudeDeg(), site.getLongitudeDeg(),
                        geo.inclinationDeg(), req.targetRaanDeg(), geo.azimuth().ascending());
                notes.add("Launch window computed for the " + (geo.azimuth().ascending() ? "ascending" : "descending")
                        + " pass over the site; windows repeat every sidereal day (23 h 56 min)");
            } else {
                notes.add("No in-plane launch window: target inclination is below the site latitude");
            }
        }

        MissionPlan plan = new MissionPlan();
        plan.setMissionName(req.missionName().trim());
        plan.setVehicle(vehicle);
        plan.setPlannedBy(users.findEntity(req.operatorId()).orElse(null));
        plan.setSite(site);
        plan.setOrbitType(req.orbitType());
        plan.setTargetAltitudeKm(req.altitudeKm());
        plan.setInclinationDeg(geo.inclinationDeg());
        plan.setPayloadKg(req.payloadKg());
        plan.setFeasible(feasible);
        plan.setLaunchAzimuthDeg(geo.azimuth().azimuthDeg());
        plan.setPitchKickDeg(best.guidance().pitchKickDeg());
        plan.setFinalPitchDeg(best.guidance().finalPitchDeg());
        plan.setMaxPayloadKg(cap.payloadKg());
        plan.setPostInsertionDvMs(extra);
        plan.setNextWindowUtc(window);
        if (shown.success()) {
            plan.setGravityLossMs(shown.gravityLoss());
            plan.setDragLossMs(shown.dragLoss());
            plan.setSteeringLossMs(shown.steeringLoss());
            plan.setAscentDvMs(shown.burnedDv());
            plan.setTotalDvMs(shown.burnedDv() + shown.circularizationDv() + extra);
            plan.setDvMarginMs(shown.margin(extra));
            plan.setMaxQkPa(shown.maxQ() / 1000);
            plan.setInsertionTimeS(shown.cutoffTimeS());
        }
        List<String> allNotes = new ArrayList<>();
        if (failure != null) allNotes.add("FAILURE: " + failure);
        allNotes.addAll(notes);
        plan.setNotes(truncate(String.join("\n", allNotes), 2000));

        if (req.save()) {
            plan = repo.save(plan);
        }
        return toResponse(plan, shown, failure, notes, shown.trajectory());
    }

    @Transactional(readOnly = true)
    public List<MissionResponse> history() {
        return repo.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> toResponse(p, null, failureOf(p), notesOf(p), null))
                .toList();
    }

    @Transactional(readOnly = true)
    public MissionResponse get(Long id) {
        MissionPlan p = getEntity(id);
        AscentResult r = replay(p);
        return toResponse(p, r, failureOf(p), notesOf(p), r.trajectory());
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(getEntity(id));
    }

    /** Re-simulates the stored plan, writes the trajectory to a CSV file and returns the file path. */
    @Transactional(readOnly = true)
    public Path exportCsv(Long id) {
        MissionPlan p = getEntity(id);
        AscentResult r = replay(p);
        try {
            return CsvExporter.write(r.trajectory(), exportDir, "mission_" + p.getId() + "_" + p.getMissionName());
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write CSV export", e);
        }
    }

    public byte[] readFile(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    private MissionPlan getEntity(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mission", id));
    }

    private AscentResult replay(MissionPlan p) {
        LaunchGeometry geo = LaunchGeometry.of(p.getSite(), p.getOrbitType(), p.getTargetAltitudeKm(),
                p.getInclinationDeg());
        return new AscentSimulator(Mapper.toSpec(p.getVehicle()), p.getPayloadKg(), geo.insertionAltM(),
                geo.azimuth().rotationAssistMs())
                .simulate(new Guidance(p.getPitchKickDeg(), p.getFinalPitchDeg()), true);
    }

    private static String failureOf(MissionPlan p) {
        if (p.getNotes() == null) return null;
        return Arrays.stream(p.getNotes().split("\n"))
                .filter(l -> l.startsWith("FAILURE: "))
                .map(l -> l.substring(9))
                .findFirst().orElse(null);
    }

    private static List<String> notesOf(MissionPlan p) {
        if (p.getNotes() == null || p.getNotes().isBlank()) return List.of();
        return Arrays.stream(p.getNotes().split("\n")).filter(l -> !l.startsWith("FAILURE: ")).toList();
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static MissionResponse toResponse(MissionPlan p, AscentResult r, String failure,
                                              List<String> notes, List<TrajectoryPoint> trajectory) {
        boolean ok = r != null && r.success();
        return new MissionResponse(
                p.getId(), p.getMissionName(), p.getCreatedAt(),
                p.getPlannedBy() != null ? p.getPlannedBy().getFullName() : null,
                p.getVehicle().getName(), p.getSite().getName(),
                p.getSite().getLatitudeDeg(), p.getSite().getLongitudeDeg(), p.getOrbitType(),
                p.getTargetAltitudeKm(), p.getInclinationDeg(), p.getPayloadKg(),
                p.isFeasible(), failure,
                p.getLaunchAzimuthDeg(), p.getPitchKickDeg(), p.getFinalPitchDeg(),
                p.getInsertionTimeS(),
                ok ? r.cutoffAltitudeM() / 1000 : 0,
                p.getGravityLossMs(), p.getDragLossMs(), p.getSteeringLossMs(),
                p.getAscentDvMs(),
                ok ? r.circularizationDv() : 0,
                p.getPostInsertionDvMs(), p.getTotalDvMs(), p.getDvMarginMs(),
                p.getMaxPayloadKg(), p.getMaxQkPa(), p.getNextWindowUtc(),
                notes, trajectory);
    }
}
