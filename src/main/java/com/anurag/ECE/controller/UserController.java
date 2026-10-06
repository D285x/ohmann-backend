package com.anurag.ECE.controller;

import com.anurag.ECE.dto.LoginRequest;
import com.anurag.ECE.dto.RegistrationRequest;
import com.anurag.ECE.dto.SessionResponse;
import com.anurag.ECE.dto.UserResponse;
import com.anurag.ECE.exception.ForbiddenException;
import com.anurag.ECE.security.SessionStore;
import com.anurag.ECE.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;
    private final SessionStore sessions;

    public UserController(UserService service, SessionStore sessions) {
        this.service = service;
        this.sessions = sessions;
    }

    /** Creates the account and logs it in straight away (the response carries a session token). */
    @PostMapping("/register")
    public ResponseEntity<SessionResponse> register(@Valid @RequestBody RegistrationRequest request) {
        UserResponse u = service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionResponse.of(u, sessions.create(u.id())));
    }

    @PostMapping("/login")
    public SessionResponse login(@Valid @RequestBody LoginRequest request) {
        UserResponse u = service.login(request);
        return SessionResponse.of(u, sessions.create(u.id()));
    }

    /** The operator behind the session token; 401 if the token is missing or expired. */
    @GetMapping("/me")
    public UserResponse me(@RequestHeader(value = "Authorization", required = false) String auth) {
        return service.get(sessions.requireUser(auth));
    }

    @GetMapping
    public List<UserResponse> list() {
        return service.findAll();
    }

    /** Operators can delete only their own account. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        Long me = sessions.requireUser(auth);
        if (!me.equals(id)) throw new ForbiddenException("You can only delete your own account");
        service.delete(id);
        sessions.revokeUser(id);
        return ResponseEntity.noContent().build();
    }
}
