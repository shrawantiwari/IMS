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
@Table(name = "user_login_audit")
public class UserLoginAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "login_at", nullable = false)
    private Instant loginAt;

    @Column(name = "logout_at")
    private Instant logoutAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "oauth_authorization_id", length = 100)
    private String oauthAuthorizationId;

    public UserLoginAuditEntity() {
    }

    public UserLoginAuditEntity(UUID userId, String email, Instant loginAt, String ipAddress,
                               String userAgent, String sessionId, String status) {
        this.userId = userId;
        this.email = email;
        this.loginAt = loginAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionId = sessionId;
        this.status = status;
    }
    public String getOauthAuthorizationId() {
        return oauthAuthorizationId;
    }

    public void setOauthAuthorizationId(String oauthAuthorizationId) {
        this.oauthAuthorizationId = oauthAuthorizationId;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getLoginAt() {
        return loginAt;
    }

    public void setLoginAt(Instant loginAt) {
        this.loginAt = loginAt;
    }

    public Instant getLogoutAt() {
        return logoutAt;
    }

    public void setLogoutAt(Instant logoutAt) {
        this.logoutAt = logoutAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
