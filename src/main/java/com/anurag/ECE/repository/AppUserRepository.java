package com.anurag.ECE.repository;

import com.anurag.ECE.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    boolean existsByEmailIgnoreCase(String email);
    java.util.Optional<AppUser> findByEmailIgnoreCase(String email);
    List<AppUser> findAllByOrderByCreatedAtDesc();
}
