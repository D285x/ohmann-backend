package com.anurag.ECE.dto;

import com.anurag.ECE.entity.UserRole;

import java.time.Instant;

/** Returned by register and login: the operator plus the session token for later requests. */
public record SessionResponse(Long id, String fullName, String email, String phone, String organization,
                              UserRole role, Instant createdAt, String token) {

    public static SessionResponse of(UserResponse u, String token) {
        return new SessionResponse(u.id(), u.fullName(), u.email(), u.phone(), u.organization(),
                u.role(), u.createdAt(), token);
    }
}
