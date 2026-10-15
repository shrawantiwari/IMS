package com.ims.authserver.controller;

import com.ims.authserver.entity.UserActiveSessionEntity;
import com.ims.authserver.entity.UserLoginAuditEntity;
import com.ims.authserver.service.ApplicationLogoutService;
import com.ims.authserver.service.OAuthAuthorizationRevocationService;
import com.ims.authserver.service.UserSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class SessionManagementController {

    private final UserSessionService userSessionService;

    private final ApplicationLogoutService applicationLogoutService;

    public SessionManagementController(
            UserSessionService userSessionService,
            ApplicationLogoutService applicationLogoutService) {

        this.userSessionService = userSessionService;
        this.applicationLogoutService = applicationLogoutService;
    }

    @GetMapping("/api/sessions/history/{userId}")
    public ResponseEntity<List<UserLoginAuditEntity>> getLoginHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(userSessionService.getLoginHistory(userId));
    }

    @GetMapping("/api/sessions/active/{userId}")
    public ResponseEntity<List<UserActiveSessionEntity>> getActiveSessions(@PathVariable UUID userId) {
        return ResponseEntity.ok(userSessionService.getActiveSessions(userId));
    }

    @PostMapping("/api/sessions/logout")
    public ResponseEntity<Map<String, String>> logoutSession(
            Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "unauthorized"));
        }

        String sessionId =
                jwtAuthentication.getToken().getClaimAsString("sid");

        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "session_not_found"));
        }

        applicationLogoutService.logoutCurrentSession(sessionId);

        return ResponseEntity.ok(
                Map.of("status", "logged_out")
        );
    }
}
