package com.anurag.ECE.service;

import com.anurag.ECE.aero.AeroClient;
import com.anurag.ECE.aero.AeroCurveResult;
import com.anurag.ECE.dto.AeroRefineRequest;
import com.anurag.ECE.dto.VehicleDto;
import com.anurag.ECE.entity.LaunchVehicle;
import com.anurag.ECE.exception.ConflictException;
import com.anurag.ECE.exception.ResourceNotFoundException;
import com.anurag.ECE.repository.LaunchVehicleRepository;
import com.anurag.ECE.repository.MissionPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class VehicleService {

    private final LaunchVehicleRepository repo;
    private final MissionPlanRepository missions;
    private final AeroClient aeroClient;

    public VehicleService(LaunchVehicleRepository repo, MissionPlanRepository missions, AeroClient aeroClient) {
        this.repo = repo;
        this.missions = missions;
        this.aeroClient = aeroClient;
    }

    @Transactional(readOnly = true)
    public List<VehicleDto> findAll() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(LaunchVehicle::getName, String.CASE_INSENSITIVE_ORDER))
                .map(Mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleDto findById(Long id) {
        return Mapper.toDto(getEntity(id));
    }

    public LaunchVehicle getEntity(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Launch vehicle", id));
    }

    @Transactional
    public VehicleDto create(VehicleDto dto) {
        if (repo.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A vehicle named '" + dto.name() + "' already exists");
        }
        LaunchVehicle v = new LaunchVehicle();
        Mapper.copy(dto, v);
        return Mapper.toDto(repo.save(v));
    }

    @Transactional
    public VehicleDto update(Long id, VehicleDto dto) {
        LaunchVehicle v = getEntity(id);
        if (!v.getName().equalsIgnoreCase(dto.name().trim()) && repo.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A vehicle named '" + dto.name() + "' already exists");
        }
        Mapper.copy(dto, v);
        return Mapper.toDto(repo.save(v));
    }

    @Transactional
    public void delete(Long id) {
        LaunchVehicle v = getEntity(id);
        if (missions.existsByVehicleId(id)) {
            throw new ConflictException("Vehicle is used by saved missions; delete those missions first");
        }
        repo.delete(v);
    }

    /**
     * Calls the aero-service to fetch a Mach-indexed drag coefficient curve
     * (shockFLOW CFD, or its analytic fallback) and stores it on the vehicle.
     * Propagates {@link com.anurag.ECE.aero.AeroServiceException} on failure;
     * the vehicle's existing constant drag coefficient is left untouched.
     */
    @Transactional
    public VehicleDto refineAero(Long id, AeroRefineRequest req) {
        LaunchVehicle v = getEntity(id);
        double fineness = req != null && req.noseFinenessRatio() != null ? req.noseFinenessRatio() : 3.0;
        double machMin = req != null && req.machMin() != null ? req.machMin() : 0.3;
        double machMax = req != null && req.machMax() != null ? req.machMax() : 5.0;
        int points = req != null && req.points() != null ? req.points() : 24;

        AeroCurveResult result = aeroClient.fetchDragCurve(v.getDiameterM(), fineness, machMin, machMax, points);
        v.setDragCurveJson(Mapper.dragCurveToJson(result.mach(), result.cd()));
        v.setAeroSource("shockflow".equalsIgnoreCase(result.source()) ? "SHOCKFLOW" : "ANALYTIC_FALLBACK");
        return Mapper.toDto(repo.save(v));
    }

    /** Discards a fetched drag curve; the vehicle goes back to its constant drag coefficient. */
    @Transactional
    public VehicleDto resetAero(Long id) {
        LaunchVehicle v = getEntity(id);
        v.setDragCurveJson(null);
        v.setAeroSource("CONSTANT");
        return Mapper.toDto(repo.save(v));
    }
}
