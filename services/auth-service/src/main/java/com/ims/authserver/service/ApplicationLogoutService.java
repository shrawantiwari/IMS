package com.ims.authserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationLogoutService {

    private final UserSessionService userSessionService;
    private final OAuthAuthorizationRevocationService
            oauthAuthorizationRevocationService;

    public ApplicationLogoutService(
            UserSessionService userSessionService,
            OAuthAuthorizationRevocationService
                    oauthAuthorizationRevocationService) {

        this.userSessionService = userSessionService;
        this.oauthAuthorizationRevocationService =
                oauthAuthorizationRevocationService;
    }

    @Transactional
    public void logoutCurrentSession(String sessionId) {

        String authorizationId =
                userSessionService.revokeSessionBySessionId(
                        sessionId,
                        "LOGOUT"
                );

        if (authorizationId != null) {
            oauthAuthorizationRevocationService
                    .revokeRefreshToken(authorizationId);
        }
    }
}