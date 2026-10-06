package com.anurag.ECE.security;

import com.anurag.ECE.exception.UnauthorizedException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal session handling: logging in issues a random bearer token, and requests that
 * must know who is asking (deleting a plan or an account) send it back in the
 * {@code Authorization: Bearer <token>} header. Tokens live in memory, so a server
 * restart logs everyone out.
 */
@Component
public class SessionStore {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "Bearer ";

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    /** Creates a new session for the operator and returns its token. */
    public String create(Long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.put(token, userId);
        return token;
    }

    /** Operator id for an {@code Authorization} header value, if it carries a live session. */
    public Optional<Long> userId(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(PREFIX)) return Optional.empty();
        return Optional.ofNullable(tokens.get(authorizationHeader.substring(PREFIX.length()).trim()));
    }

    /** Like {@link #userId} but fails with 401 when there is no live session. */
    public Long requireUser(String authorizationHeader) {
        return userId(authorizationHeader).orElseThrow(() ->
                new UnauthorizedException("You are not logged in, or your session has expired. Please log in again."));
    }

    /** Ends every session of an operator (used when the account is deleted). */
    public void revokeUser(Long userId) {
        tokens.values().removeIf(userId::equals);
    }
}
