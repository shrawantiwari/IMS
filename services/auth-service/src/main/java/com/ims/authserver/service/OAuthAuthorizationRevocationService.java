package com.ims.authserver.service;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthAuthorizationRevocationService {

    private final OAuth2AuthorizationService authorizationService;

    public OAuthAuthorizationRevocationService(
            OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Transactional
    public void revokeRefreshToken(String authorizationId) {

        if (authorizationId == null || authorizationId.isBlank()) {
            return;
        }

        OAuth2Authorization authorization =
                authorizationService.findById(authorizationId);

        if (authorization == null) {
            return;
        }

        if (authorization.getRefreshToken() == null) {
            return;
        }

        OAuth2Authorization updatedAuthorization =
                OAuth2Authorization.from(authorization)
                        .invalidate(authorization.getRefreshToken().getToken())
                        .build();

        authorizationService.save(updatedAuthorization);
    }
}