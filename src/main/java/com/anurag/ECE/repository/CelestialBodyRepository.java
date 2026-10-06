package com.anurag.ECE.repository;

import com.anurag.ECE.entity.CelestialBody;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CelestialBodyRepository extends JpaRepository<CelestialBody, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<CelestialBody> findAllByOrderBySemiMajorAxisAuAsc();
}
