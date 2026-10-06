package com.anurag.ECE.repository;

import com.anurag.ECE.entity.LaunchVehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaunchVehicleRepository extends JpaRepository<LaunchVehicle, Long> {
    boolean existsByNameIgnoreCase(String name);
}
