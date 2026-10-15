package com.ims.authserver.config;

import com.ims.authserver.repository.UserRepository;
import com.ims.authserver.service.SessionAwareOAuth2AuthorizationService;
import com.ims.authserver.service.UserSessionService;
import com.ims.authserver.service.UserSessionUserResolver;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;


@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class AuthServerConfig {

    @Value("${ims.auth.access-token-ttl:5m}")
    private Duration accessTokenTtl;

    @Value("${ims.auth.refresh-token-ttl:7d}")
    private Duration refreshTokenTtl;

    @Value("${ims.auth.client-id:inventory-client}")
    private String clientId;

    @Value("${ims.auth.redirect-uri:http://localhost:4200/oauth/callback}")
    private String redirectUri;

    @Value("${ims.auth.allowed-origin:http://localhost:4200}")
    private String allowedOrigin;

    @Value("${ims.auth.issuer:http://localhost:9000}")
    private String issuer;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2TokenGenerator<OAuth2Token> oauth2TokenGenerator) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .securityMatcher("/.well-known/**", "/oauth2/**", "/login/oauth2/code/*");
        http.oauth2AuthorizationServer(authorizationServer -> authorizationServer
                .oidc(Customizer.withDefaults())
                .tokenGenerator(oauth2TokenGenerator));

        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")));

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/.well-known/**", "/oauth2/**").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            UserSessionService userSessionService,
            SecurityContextRepository securityContextRepository) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)

                .securityContext(securityContext ->
                        securityContext
                                .securityContextRepository(
                                        securityContextRepository
                                )
                                .requireExplicitSave(true)
                )

                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/login",
                                "/error",
                                "/actuator/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(
                                successHandler(
                                        userSessionService,
                                        securityContextRepository
                                )
                        )
                        .failureHandler(failureHandler())
                        .permitAll()
                )

                .oauth2ResourceServer(oauth2ResourceServer ->
                        oauth2ResourceServer
                                .jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT,
                HttpHeaders.ORIGIN,
                HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "X-Requested-With"));
        configuration.setExposedHeaders(List.of(HttpHeaders.AUTHORIZATION));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationSuccessHandler successHandler(
            UserSessionService userSessionService,
            SecurityContextRepository securityContextRepository) {

        HttpSessionRequestCache requestCache =
                new HttpSessionRequestCache();

        SavedRequestAwareAuthenticationSuccessHandler
                savedRequestHandler =
                new SavedRequestAwareAuthenticationSuccessHandler();

        savedRequestHandler.setRequestCache(requestCache);

        return (request, response, authentication) -> {

            /*
             * Persist authenticated user in HTTP session.
             *
             * This is important because the next request will be:
             *
             * /oauth2/authorize
             *
             * and Spring Authorization Server needs to see the
             * authenticated user on that new request.
             */
            org.springframework.security.core.context.SecurityContext securityContext =
                    org.springframework.security.core.context.SecurityContextHolder
                            .createEmptyContext();

            securityContext.setAuthentication(authentication);

            org.springframework.security.core.context.SecurityContextHolder
                    .setContext(securityContext);

            securityContextRepository.saveContext(
                    securityContext,
                    request,
                    response
            );

            /*
             * Record login audit.
             */
            String sessionId =
                    request.getSession(false) != null
                            ? request.getSession(false).getId()
                            : UUID.randomUUID().toString();

            userSessionService.recordSuccessfulLogin(
                    authentication.getName(),
                    sessionId,
                    request.getRemoteAddr(),
                    request.getHeader(HttpHeaders.USER_AGENT)
            );

            /*
             * Continue the original OAuth authorization request.
             */
            if (requestCache.getRequest(request, response) != null) {

                savedRequestHandler.onAuthenticationSuccess(
                        request,
                        response,
                        authentication
                );

                return;
            }

            /*
             * Normal non-OAuth login.
             */
            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            response.setContentType(
                    MediaType.APPLICATION_JSON_VALUE
            );

            response.setCharacterEncoding("UTF-8");

            response.getWriter().write(
                    "{\"status\":\"authenticated\"}"
            );
        };
    }

    @Bean
    public AuthenticationFailureHandler failureHandler() {
        return (_, response, _) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Authentication failed\"}");
        };
    }

    @Bean
    public TokenSettings tokenSettings() {
        return TokenSettings.builder()
                .accessTokenFormat(OAuth2TokenFormat.SELF_CONTAINED)
                .accessTokenTimeToLive(accessTokenTtl)
                .reuseRefreshTokens(false)
                .refreshTokenTimeToLive(refreshTokenTtl)
                .build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            JdbcTemplate jdbcTemplate) {

        JdbcRegisteredClientRepository repository =
                new JdbcRegisteredClientRepository(jdbcTemplate);

        RegisteredClient inventoryClient = RegisteredClient.withId("inventory-client")
                .clientId(clientId)
                .clientName("Inventory Management System")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("offline_access")
                .clientSettings(
                        ClientSettings.builder()
                                .requireProofKey(true)
                                .requireAuthorizationConsent(false)
                                .build()
                )
                .tokenSettings(
                        TokenSettings.builder()
                                .accessTokenTimeToLive(accessTokenTtl)
                                .refreshTokenTimeToLive(refreshTokenTtl)
                                .reuseRefreshTokens(false)
                                .build()
                )
                .build();

        if (repository.findByClientId(clientId) == null) {
            repository.save(inventoryClient);
        }

        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository,
            UserSessionService userSessionService,
            UserSessionUserResolver userSessionUserResolver,
            ObjectProvider<HttpServletRequest> requestProvider) {

        OAuth2AuthorizationService jdbcAuthorizationService =
                new JdbcOAuth2AuthorizationService(
                        jdbcTemplate,
                        registeredClientRepository
                );

        return new SessionAwareOAuth2AuthorizationService(
                jdbcAuthorizationService,
                userSessionService,
                userSessionUserResolver,
                requestProvider
        );
    }
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(
                jdbcTemplate,
                registeredClientRepository
        );
    }

    @Bean
    public OAuth2TokenGenerator<OAuth2Token> oauth2TokenGenerator(
            JWKSource<SecurityContext> jwkSource,
            OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer) {

        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        jwtGenerator.setJwtCustomizer(jwtCustomizer);

        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();

        OAuth2TokenGenerator<OAuth2RefreshToken> refreshTokenGenerator = context -> {
            if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
                return null;
            }

            if (!AuthorizationGrantType.AUTHORIZATION_CODE.equals(context.getAuthorizationGrantType())) {
                return null;
            }

            context.getRegisteredClient();
            String clientId = context.getRegisteredClient().getClientId();
            if (!AuthServerConfig.this.clientId.equals(clientId)) {
                return null;
            }

            java.time.Instant issuedAt = java.time.Clock.systemUTC().instant();
            java.time.Instant expiresAt = issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
            return new OAuth2RefreshToken(
                    new org.springframework.security.crypto.keygen.Base64StringKeyGenerator(
                            java.util.Base64.getUrlEncoder().withoutPadding(), 96).generateKey(),
                    issuedAt,
                    expiresAt);
        };

        return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(
            UserRepository userRepository) {

        return context -> {

            Authentication principal = context.getPrincipal();

            if (principal == null || principal.getName() == null) {
                return;
            }

            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())
                    && !"id_token".equals(context.getTokenType().getValue())) {
                return;
            }

            userRepository.findByEmail(principal.getName()).ifPresent(user -> {

                String normalizedRole =
                        (user.getRole() == null || user.getRole().isBlank())
                                ? "USER"
                                : user.getRole().trim().toUpperCase(Locale.ROOT);

                context.getClaims().claims(claims -> {

                    claims.put("userId", user.getId().toString());
                    claims.put("email", user.getEmail());

                    claims.put(
                            "roles",
                            new ArrayList<>(
                                    List.of("ROLE_" + normalizedRole)
                            )
                    );

                    if (context.getAuthorization() != null) {

                        String applicationSessionId =
                                context.getAuthorization()
                                        .getAttribute("application_session_id");

                        if (applicationSessionId != null
                                && !applicationSessionId.isBlank()) {

                            claims.put("sid", applicationSessionId);
                        }
                    }
                });
            });
        };
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws NoSuchAlgorithmException {
        // For absolute production safety, replace this generated RSA keypair with a keystore-backed
        // JWKSource loaded from an external .jks/.p12 file (for example, by reading a KeyStore from a
        // mounted secret or Cloud KMS-managed key material) instead of generating ephemeral keys in-memory.
        RSAKey rsaKey = generateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    private RSAKey generateRsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }
}
