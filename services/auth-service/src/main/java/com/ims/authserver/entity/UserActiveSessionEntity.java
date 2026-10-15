package com.ims.authserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_active_session")
public class UserActiveSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "oauth_authorization_id", length = 100)
    private String oauthAuthorizationId;

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    private String refreshTokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    @Column(name = "device_name", length = 255)
    private String deviceName;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserActiveSessionEntity() {
    }

    public UserActiveSessionEntity(
            UUID userId,
            String oauthAuthorizationId,
            String refreshTokenHash,
            Instant issuedAt,
            Instant expiresAt,
            String deviceName,
            String sessionId) {

        this.userId = userId;
        this.oauthAuthorizationId = oauthAuthorizationId;
        this.refreshTokenHash = refreshTokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.deviceName = deviceName;
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getOauthAuthorizationId() {
        return oauthAuthorizationId;
    }

    public void setOauthAuthorizationId(String oauthAuthorizationId) {
        this.oauthAuthorizationId = oauthAuthorizationId;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getRevokedReason() {
        return revokedReason;
    }

    public void setRevokedReason(String revokedReason) {
        this.revokedReason = revokedReason;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
