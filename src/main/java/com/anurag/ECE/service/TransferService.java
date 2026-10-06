package com.anurag.ECE.service;

import com.anurag.ECE.dto.TransferRequest;
import com.anurag.ECE.dto.TransferResponse;
import com.anurag.ECE.dto.TransferSummary;
import com.anurag.ECE.dto.TransferWindowDto;
import com.anurag.ECE.entity.*;
import com.anurag.ECE.exception.InvalidRequestException;
import com.anurag.ECE.exception.ResourceNotFoundException;
import com.anurag.ECE.physics.*;
import com.anurag.ECE.repository.TransferPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class TransferService {

    private static final double MAX_PARKING_FOR_CAPACITY_KM = 2000;

    private final TransferPlanRepository repo;
    private final BodyService bodies;
    private final VehicleService vehicles;
    private final SiteService sites;
    private final UserService users;
    private final AscentOptimizer optimizer;

    public TransferService(TransferPlanRepository repo, BodyService bodies, VehicleService vehicles,
                           SiteService sites, UserService users, AscentOptimizer optimizer) {
        this.repo = repo;
        this.bodies = bodies;
        this.vehicles = vehicles;
        this.sites = sites;
        this.users = users;
        this.optimizer = optimizer;
    }

    @Transactional
    public TransferResponse plan(TransferRequest req) {
        if (req.originId().equals(req.destinationId())) {
            throw new InvalidRequestException("Origin and destination must be different bodies");
        }
        CelestialBody originEntity = bodies.getEntity(req.originId());
        CelestialBody destEntity = bodies.getEntity(req.destinationId());
        Body origin = originEntity.toBody();
        Body dest = destEntity.toBody();

        Instant from = req.earliestDepartureUtc() != null ? req.earliestDepartureUtc() : Instant.now();
        int count = req.windowCount() != null ? req.windowCount() : 3;
        double captureAlt = req.captureAltitudeKm() != null ? req.captureAltitudeKm() * 1000 : -1;

        List<InterplanetaryPlanner.Window> windows = InterplanetaryPlanner.windows(
                origin, dest, from, count, req.parkingAltitudeKm() * 1000, captureAlt);
        List<TransferWindowDto> dtos = windows.stream()
                .map(w -> new TransferWindowDto(w.departure(), w.arrival(), w.timeOfFlightDays(),
                        w.phaseAngleDeg(), w.c3Km2s2(), w.vInfDepartureMs(), w.vInfArrivalMs(),
                        w.departureDvMs(), w.arrivalDvMs(), w.totalDvMs()))
                .toList();

        List<String> notes = new ArrayList<>();
        notes.add("Model: circular coplanar orbits, Hohmann transfer, patched conics. "
                + "Real windows shift by days to weeks because of orbital eccentricity and inclination.");
        if (captureAlt < 0) notes.add("No capture orbit requested: arrival delta-v is zero (flyby)");

        String vehicleName = null, siteName = null;
        Double capacity = null;
        if (req.vehicleId() != null) {
            if (!originEntity.isEarth()) {
                notes.add("Launch vehicle capacity is only evaluated for departures from Earth");
            } else if (req.siteId() == null) {
                throw new InvalidRequestException("Select a launch site to estimate payload capacity");
            } else {
                LaunchVehicle v = vehicles.getEntity(req.vehicleId());
                LaunchSite s = sites.getEntity(req.siteId());
                vehicleName = v.getName();
                siteName = s.getName();
                if (req.parkingAltitudeKm() > MAX_PARKING_FOR_CAPACITY_KM) {
                    notes.add("Payload capacity needs a parking orbit of 2000 km or lower");
                } else {
                    capacity = departureCapacity(v, s, req.parkingAltitudeKm(), windows.get(0).departureDvMs());
                    notes.add(String.format(Locale.ROOT,
                            "Payload estimate: %s launches due east into a %.0f km parking orbit, "
                                    + "then its upper stage performs the %.0f m/s departure burn",
                            v.getName(), req.parkingAltitudeKm(), windows.get(0).departureDvMs()));
                    if (capacity <= 0) notes.add("This vehicle cannot reach the required departure energy");
                }
            }
        }

        Long savedId = null;
        if (req.save()) {
            InterplanetaryPlanner.Window w = windows.get(0);
            TransferPlan p = new TransferPlan();
            p.setOrigin(originEntity.getName());
            p.setDestination(destEntity.getName());
            p.setDepartureUtc(w.departure());
            p.setArrivalUtc(w.arrival());
            p.setTimeOfFlightDays(w.timeOfFlightDays());
            p.setPhaseAngleDeg(w.phaseAngleDeg());
            p.setC3Km2s2(w.c3Km2s2());
            p.setDepartureDvMs(w.departureDvMs());
            p.setArrivalDvMs(w.arrivalDvMs());
            p.setTotalDvMs(w.totalDvMs());
            p.setVehicleName(vehicleName);
            p.setPayloadCapacityKg(capacity);
            if (req.operatorId() != null) p.setPlannedBy(users.getEntity(req.operatorId()));
            savedId = repo.save(p).getId();
        }

        return new TransferResponse(savedId, originEntity.getName(), destEntity.getName(),
                InterplanetaryPlanner.synodicPeriodDays(origin, dest), dtos, vehicleName, siteName, capacity, notes);
    }

    private double departureCapacity(LaunchVehicle v, LaunchSite s, double parkingKm, double departureDv) {
        LaunchGeometry geo = LaunchGeometry.of(s, OrbitType.LEO, parkingKm, null);
        VehicleSpec spec = Mapper.toSpec(v);
        double rot = geo.azimuth().rotationAssistMs();
        AscentResult seed = optimizer.optimize(spec, 0, geo.insertionAltM(), rot, departureDv);
        return optimizer.payloadCapacity(spec, geo.insertionAltM(), rot, departureDv, seed.guidance()).payloadKg();
    }

    @Transactional(readOnly = true)
    public List<TransferSummary> history() {
        return repo.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> new TransferSummary(p.getId(), p.getCreatedAt(), p.getOrigin(), p.getDestination(),
                        p.getDepartureUtc(), p.getArrivalUtc(), p.getTimeOfFlightDays(), p.getC3Km2s2(),
                        p.getDepartureDvMs(), p.getArrivalDvMs(), p.getTotalDvMs(), p.getVehicleName(),
                        p.getPayloadCapacityKg(),
                        p.getPlannedBy() != null ? p.getPlannedBy().getFullName() : null))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        TransferPlan p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transfer plan", id));
        repo.delete(p);
    }
}
