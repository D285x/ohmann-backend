package com.anurag.ECE.repository;

import com.anurag.ECE.entity.TransferPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferPlanRepository extends JpaRepository<TransferPlan, Long> {
    java.util.List<TransferPlan> findAllByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TransferPlan t set t.plannedBy = null where t.plannedBy.id = :userId")
    int detachOperator(@org.springframework.data.repository.query.Param("userId") Long userId);
}
