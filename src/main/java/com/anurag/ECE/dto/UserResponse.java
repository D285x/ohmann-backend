package com.anurag.ECE.dto;

import com.anurag.ECE.entity.UserRole;

import java.time.Instant;

public record UserResponse(Long id, String fullName, String email, String phone, String organization,
                           UserRole role, Instant createdAt) {
}
