package com.ims.authserver.repository;

import com.ims.authserver.entity.UserLoginAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserLoginAuditRepository extends JpaRepository<UserLoginAuditEntity, Long> {

    List<UserLoginAuditEntity> findByUserIdOrderByLoginAtDesc(UUID userId);
    List<UserLoginAuditEntity> findByOauthAuthorizationId(
            String oauthAuthorizationId);
    List<UserLoginAuditEntity> findBySessionId(String sessionId);
}
