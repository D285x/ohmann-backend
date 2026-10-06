package com.anurag.ECE.service;

import com.anurag.ECE.dto.BodyDto;
import com.anurag.ECE.entity.CelestialBody;
import com.anurag.ECE.exception.ConflictException;
import com.anurag.ECE.exception.ResourceNotFoundException;
import com.anurag.ECE.physics.Body;
import com.anurag.ECE.physics.OrbitalMechanics;
import com.anurag.ECE.repository.CelestialBodyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BodyService {

    private final CelestialBodyRepository repo;

    public BodyService(CelestialBodyRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<BodyDto> findAll() {
        double jd = OrbitalMechanics.julianDate(Instant.now());
        return repo.findAllByOrderBySemiMajorAxisAuAsc().stream().map(b -> toDto(b, jd)).toList();
    }

    public CelestialBody getEntity(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Celestial body", id));
    }

    @Transactional
    public BodyDto create(BodyDto dto) {
        if (repo.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A body named '" + dto.name() + "' already exists");
        }
        CelestialBody b = new CelestialBody();
        copy(dto, b);
        return toDto(repo.save(b), OrbitalMechanics.julianDate(Instant.now()));
    }

    @Transactional
    public BodyDto update(Long id, BodyDto dto) {
        CelestialBody b = getEntity(id);
        if (!b.getName().equalsIgnoreCase(dto.name().trim()) && repo.existsByNameIgnoreCase(dto.name().trim())) {
            throw new ConflictException("A body named '" + dto.name() + "' already exists");
        }
        copy(dto, b);
        return toDto(repo.save(b), OrbitalMechanics.julianDate(Instant.now()));
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(getEntity(id));
    }

    private static void copy(BodyDto dto, CelestialBody b) {
        b.setName(dto.name().trim());
        b.setBodyType(dto.bodyType());
        b.setSemiMajorAxisAu(dto.semiMajorAxisAu());
        b.setMeanLongitudeJ2000Deg(dto.meanLongitudeJ2000Deg());
        b.setGmKm3s2(dto.gmKm3s2());
        b.setRadiusKm(dto.radiusKm());
    }

    private static BodyDto toDto(CelestialBody e, double jd) {
        Body b = e.toBody();
        return new BodyDto(e.getId(), e.getName(), e.getBodyType(), e.getSemiMajorAxisAu(),
                e.getMeanLongitudeJ2000Deg(), e.getGmKm3s2(), e.getRadiusKm(),
                b.periodDays(), b.surfaceGravity(), b.meanLongitudeAt(jd));
    }
}
