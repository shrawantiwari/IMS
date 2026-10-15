package com.ims.authserver.repository;

import com.ims.authserver.entity.UserActiveSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserActiveSessionRepository
        extends JpaRepository<UserActiveSessionEntity, Long> {

    List<UserActiveSessionEntity>
    findByUserIdAndRevokedAtIsNullOrderByIssuedAtDesc(UUID userId);

    Optional<UserActiveSessionEntity>
    findBySessionIdAndRevokedAtIsNull(String sessionId);

    Optional<UserActiveSessionEntity>
    findByOauthAuthorizationIdAndRevokedAtIsNull(
            String oauthAuthorizationId);

    Optional<UserActiveSessionEntity>
    findByOauthAuthorizationId(String oauthAuthorizationId);
}
