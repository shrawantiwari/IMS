package com.ims.authserver.service;

import com.ims.authserver.entity.UserActiveSessionEntity;
import com.ims.authserver.entity.UserEntity;
import com.ims.authserver.entity.UserLoginAuditEntity;
import com.ims.authserver.repository.UserActiveSessionRepository;
import com.ims.authserver.repository.UserLoginAuditRepository;
import com.ims.authserver.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserSessionService {

    private final UserRepository userRepository;
    private final UserLoginAuditRepository userLoginAuditRepository;
    private final UserActiveSessionRepository userActiveSessionRepository;
    private final int maxSessionsPerUser;
    private final Duration sessionExpiry;

    public UserSessionService(
            UserRepository userRepository,
            UserLoginAuditRepository userLoginAuditRepository,
            UserActiveSessionRepository userActiveSessionRepository,
            @Value("${ims.auth.max-sessions-per-user:1}") int maxSessionsPerUser,
            @Value("${ims.auth.session-expiry-hours:24}") long sessionExpiryHours) {

        this.userRepository = userRepository;
        this.userLoginAuditRepository = userLoginAuditRepository;
        this.userActiveSessionRepository = userActiveSessionRepository;
        this.maxSessionsPerUser = maxSessionsPerUser;
        this.sessionExpiry = Duration.ofHours(sessionExpiryHours);
    }

    @Transactional
    public void recordSuccessfulLogin(
            String email,
            String sessionId,
            String ipAddress,
            String userAgent) {

        UserEntity user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return;
        }

        UserLoginAuditEntity audit = new UserLoginAuditEntity(
                user.getId(),
                user.getEmail(),
                Instant.now(),
                ipAddress,
                userAgent,
                sessionId,
                "SUCCESS"
        );

        userLoginAuditRepository.save(audit);
    }

    /**
     * Creates the application session associated with an OAuth authorization.
     *
     * Important:
     * - oauthAuthorizationId is the stable relationship with oauth2_authorization.
     * - refresh token changes during rotation, so we update the existing session.
     * - revoked sessions must never be recreated.
     */
    @Transactional
    public void createApplicationSession(
            UUID userId,
            String authorizationId,
            String applicationSessionId,
            String refreshToken,
            Instant issuedAt,
            Instant expiresAt,
            String deviceName
            ) {

        if (userId == null
                || authorizationId == null
                || authorizationId.isBlank()
                || applicationSessionId == null
                || applicationSessionId.isBlank()
                || refreshToken == null
                || refreshToken.isBlank()) {
            return;
        }

        String refreshTokenHash = hashRefreshToken(refreshToken);

        /*
         * OAuth authorization ID is the stable key.
         *
         * Do NOT use:
         *
         * findBySessionIdAndRevokedAtIsNull(...)
         *
         * here because a revoked session would no longer be found,
         * causing logout/revocation to create a new row.
         */
        Optional<UserActiveSessionEntity> existingSession =
                userActiveSessionRepository
                        .findByOauthAuthorizationId(authorizationId);

        if (existingSession.isPresent()) {

            UserActiveSessionEntity session = existingSession.get();

            /*
             * This OAuth authorization already belongs to a revoked
             * application session.
             *
             * This can happen when logout invalidates the authorization
             * and the OAuth authorization service saves it again.
             *
             * Never recreate the application session.
             */
            if (session.getRevokedAt() != null) {
                return;
            }

            /*
             * Refresh-token rotation:
             *
             * reuseRefreshTokens(false)
             *
             * means the refresh token changes.
             *
             * Therefore update the stored hash and expiration instead
             * of creating another application-session row.
             */
            session.setRefreshTokenHash(refreshTokenHash);
            session.setExpiresAt(expiresAt);

            userActiveSessionRepository.save(session);

            return;
        }

        /*
         * No application session exists for this OAuth authorization.
         *
         * This is the initial session creation.
         */
        enforceSingleSession(userId);

        UserActiveSessionEntity activeSession =
                new UserActiveSessionEntity(
                        userId,
                        authorizationId,
                        refreshTokenHash,
                        issuedAt,
                        expiresAt,
                        deviceName,
                        applicationSessionId
                );

        userActiveSessionRepository.save(activeSession);
    }



    @Transactional
    public void enforceSingleSession(UUID userId) {

        if (maxSessionsPerUser <= 0) {
            return;
        }

        List<UserActiveSessionEntity> sessions =
                userActiveSessionRepository
                        .findByUserIdAndRevokedAtIsNullOrderByIssuedAtDesc(userId);

        if (sessions.size() < maxSessionsPerUser) {
            return;
        }

        for (int i = maxSessionsPerUser - 1;
             i < sessions.size();
             i++) {

            UserActiveSessionEntity session = sessions.get(i);

            session.setRevokedAt(Instant.now());
            session.setRevokedReason("MAX_SESSIONS_EXCEEDED");

            userActiveSessionRepository.save(session);

            userLoginAuditRepository
                    .findByOauthAuthorizationId(
                            session.getOauthAuthorizationId())
                    .stream()
                    .filter(entry -> entry.getLogoutAt() == null)
                    .forEach(entry -> {
                        entry.setLogoutAt(Instant.now());
                        entry.setStatus("REVOKED");
                        userLoginAuditRepository.save(entry);
                    });
        }
    }

    public List<UserLoginAuditEntity> getLoginHistory(UUID userId) {

        return userLoginAuditRepository
                .findByUserIdOrderByLoginAtDesc(userId);
    }

    public List<UserActiveSessionEntity> getActiveSessions(UUID userId) {

        return userActiveSessionRepository
                .findByUserIdAndRevokedAtIsNullOrderByIssuedAtDesc(userId);
    }

    private String hashRefreshToken(String refreshToken) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            refreshToken.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hex =
                    new StringBuilder(hash.length * 2);

            for (byte b : hash) {
                hex.append(
                        String.format("%02x", b)
                );
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException ex) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    ex
            );
        }
    }



    @Transactional
    public String revokeSessionBySessionId(
            String sessionId,
            String reason) {

        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        Optional<UserActiveSessionEntity> sessionOptional =
                userActiveSessionRepository
                        .findBySessionIdAndRevokedAtIsNull(sessionId);

        if (sessionOptional.isEmpty()) {
            return null;
        }

        UserActiveSessionEntity session =
                sessionOptional.get();

        session.setRevokedAt(Instant.now());

        session.setRevokedReason(
                reason == null || reason.isBlank()
                        ? "LOGOUT"
                        : reason.trim()
        );

        userActiveSessionRepository.save(session);

        // Update the audit belonging to this OAuth authorization
        userLoginAuditRepository
                .findByOauthAuthorizationId(
                        session.getOauthAuthorizationId())
                .stream()
                .filter(entry ->
                        entry.getLogoutAt() == null)
                .forEach(entry -> {

                    entry.setLogoutAt(Instant.now());
                    entry.setStatus("LOGOUT");

                    userLoginAuditRepository.save(entry);
                });

        return session.getOauthAuthorizationId();
    }
    @Transactional
    public void bindAuditToOAuthAuthorization(
            String httpSessionId,
            String authorizationId) {

        if (httpSessionId == null
                || httpSessionId.isBlank()
                || authorizationId == null
                || authorizationId.isBlank()) {
            return;
        }

        userLoginAuditRepository
                .findBySessionId(httpSessionId)
                .stream()
                .filter(audit ->
                        audit.getLogoutAt() == null
                                && "SUCCESS".equals(audit.getStatus()))
                .findFirst()
                .ifPresent(audit -> {

                    audit.setOauthAuthorizationId(
                            authorizationId
                    );

                    userLoginAuditRepository.save(audit);
                });
    }
}