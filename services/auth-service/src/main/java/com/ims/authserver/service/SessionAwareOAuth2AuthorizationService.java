package com.ims.authserver.service;

import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;


import java.time.Instant;
import java.util.UUID;

public class SessionAwareOAuth2AuthorizationService
        implements OAuth2AuthorizationService {

    private static final String APPLICATION_SESSION_ID =
            "application_session_id";

    private final OAuth2AuthorizationService delegate;
    private final UserSessionService userSessionService;
    private final UserSessionUserResolver userSessionUserResolver;
    private final ObjectProvider<HttpServletRequest> requestProvider;


    public SessionAwareOAuth2AuthorizationService(
            OAuth2AuthorizationService delegate,
            UserSessionService userSessionService,
            UserSessionUserResolver userSessionUserResolver,
            ObjectProvider<HttpServletRequest> requestProvider) {

        this.delegate = delegate;
        this.userSessionService = userSessionService;
        this.userSessionUserResolver = userSessionUserResolver;
        this.requestProvider = requestProvider;
    }

    @Override
    public void save(OAuth2Authorization authorization) {

        String applicationSessionId =
                authorization.getAttribute(APPLICATION_SESSION_ID);

        if (applicationSessionId == null) {

            applicationSessionId = UUID.randomUUID().toString();

            authorization = OAuth2Authorization.from(authorization)
                    .attribute(
                            APPLICATION_SESSION_ID,
                            applicationSessionId
                    )
                    .build();
        }

        delegate.save(authorization);

        bindAuditToAuthorization(authorization);

        createApplicationSessionIfRefreshTokenExists(
                authorization,
                applicationSessionId
        );
    }

    private void createApplicationSessionIfRefreshTokenExists(
            OAuth2Authorization authorization,
            String applicationSessionId) {

        if (authorization.getRefreshToken() == null) {
            return;
        }

        String refreshToken =
                authorization.getRefreshToken()
                        .getToken()
                        .getTokenValue();

        Instant issuedAt =
                authorization.getRefreshToken()
                        .getToken()
                        .getIssuedAt();

        Instant expiresAt =
                authorization.getRefreshToken()
                        .getToken()
                        .getExpiresAt();

        String principalName =
                authorization.getPrincipalName();

        UUID userId =
                userSessionUserResolver.resolveUserId(principalName);

        if (userId == null) {
            return;
        }

        userSessionService.createApplicationSession(
                userId,
                authorization.getId(),
                applicationSessionId,
                refreshToken,
                issuedAt,
                expiresAt,
                null
        );
    }

    @Override
    public OAuth2Authorization findById(String id) {
        return delegate.findById(id);
    }

    @Override
    public OAuth2Authorization findByToken(
            String token,
            OAuth2TokenType tokenType) {

        return delegate.findByToken(token, tokenType);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        delegate.remove(authorization);
    }
    private void bindAuditToAuthorization(
            OAuth2Authorization authorization) {

        HttpServletRequest request =
                requestProvider.getIfAvailable();

        if (request == null) {
            return;
        }

        String httpSessionId =
                request.getSession(false) != null
                        ? request.getSession(false).getId()
                        : null;

        if (httpSessionId == null || httpSessionId.isBlank()) {
            return;
        }

        userSessionService.bindAuditToOAuthAuthorization(
                httpSessionId,
                authorization.getId()
        );
    }
}