package com.anurag.ECE.repository;

import com.anurag.ECE.entity.MissionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MissionPlanRepository extends JpaRepository<MissionPlan, Long> {
    java.util.List<MissionPlan> findAllByOrderByCreatedAtDesc();
    boolean existsByVehicleId(Long vehicleId);
    boolean existsBySiteId(Long siteId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update MissionPlan m set m.plannedBy = null where m.plannedBy.id = :userId")
    int detachOperator(@org.springframework.data.repository.query.Param("userId") Long userId);
}
