package rentsphere.auth;

import java.time.Instant;

/**
 * Cryptographically Secure User Session Entity.
 * Tracks session token, user ID, role, creation and expiration timestamps.
 */
public class UserSession {
    private final String token;
    private final String userId;
    private final String role;
    private final long createdAt;
    private final long expiresAt;
    private long lastAccessedAt;

    public UserSession(String token, String userId, String role, long durationMillis) {
        this.token = token;
        this.userId = userId;
        this.role = role;
        this.createdAt = Instant.now().toEpochMilli();
        this.expiresAt = this.createdAt + durationMillis;
        this.lastAccessedAt = this.createdAt;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public long getLastAccessedAt() {
        return lastAccessedAt;
    }

    public synchronized void touch() {
        this.lastAccessedAt = Instant.now().toEpochMilli();
    }

    public boolean isExpired() {
        return Instant.now().toEpochMilli() > expiresAt;
    }
}
