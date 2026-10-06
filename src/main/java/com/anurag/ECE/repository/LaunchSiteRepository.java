package com.anurag.ECE.repository;

import com.anurag.ECE.entity.LaunchSite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaunchSiteRepository extends JpaRepository<LaunchSite, Long> {
    boolean existsByNameIgnoreCase(String name);
}
