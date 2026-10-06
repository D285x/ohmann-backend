package com.anurag.ECE.service;

import com.anurag.ECE.dto.LoginRequest;
import com.anurag.ECE.dto.RegistrationRequest;
import com.anurag.ECE.dto.UserResponse;
import com.anurag.ECE.entity.AppUser;
import com.anurag.ECE.exception.ConflictException;
import com.anurag.ECE.exception.ResourceNotFoundException;
import com.anurag.ECE.exception.UnauthorizedException;
import com.anurag.ECE.repository.AppUserRepository;
import com.anurag.ECE.repository.MissionPlanRepository;
import com.anurag.ECE.repository.TransferPlanRepository;
import com.anurag.ECE.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final AppUserRepository repo;
    private final MissionPlanRepository missions;
    private final TransferPlanRepository transfers;

    public UserService(AppUserRepository repo, MissionPlanRepository missions, TransferPlanRepository transfers) {
        this.repo = repo;
        this.missions = missions;
        this.transfers = transfers;
    }

    public AppUser getEntity(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Operator", id));
    }

    /**
     * Operator lookup for attributing a saved plan. Returns empty instead of failing when
     * the id no longer exists (e.g. a browser still remembers a login from before the
     * in-memory demo database was reset), so the plan is still saved, just unattributed.
     */
    public Optional<AppUser> findEntity(Long id) {
        return id == null ? Optional.empty() : repo.findById(id);
    }

    /** Checks the password against the stored salted hash (constant-time comparison). */
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest req) {
        AppUser u = repo.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        String attempt = PasswordHasher.hash(req.password(), u.getPasswordSalt());
        boolean ok = MessageDigest.isEqual(attempt.getBytes(StandardCharsets.UTF_8),
                u.getPasswordHash().getBytes(StandardCharsets.UTF_8));
        if (!ok) throw new UnauthorizedException("Invalid email or password");
        return toResponse(u);
    }

    @Transactional
    public UserResponse register(RegistrationRequest req) {
        String email = req.email().trim().toLowerCase();
        if (repo.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with " + email + " already exists");
        }
        AppUser u = new AppUser();
        u.setFullName(req.fullName().trim());
        u.setEmail(email);
        u.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        u.setOrganization(req.organization());
        u.setRole(req.role());
        String salt = PasswordHasher.newSalt();
        u.setPasswordSalt(salt);
        u.setPasswordHash(PasswordHasher.hash(req.password(), salt));
        return toResponse(repo.save(u));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(UserService::toResponse).toList();
    }

    @Transactional
    public void delete(Long id) {
        AppUser u = getEntity(id);
        missions.detachOperator(id);     // keep the plans, just remove the attribution
        transfers.detachOperator(id);
        repo.delete(u);
    }

    private static UserResponse toResponse(AppUser u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getOrganization(),
                u.getRole(), u.getCreatedAt());
    }
}
