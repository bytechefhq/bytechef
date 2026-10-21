/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.security;

import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.APP_USE_SCOPE;
import static com.bytechef.platform.oauth2.authorizationserver.constant.OAuth2AuthorizationServerConstants.TENANT_ID_CLAIM;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.app.domain.AppInstance;
import com.bytechef.ee.automation.app.exception.AppErrorType;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.mcp.SelfIssuedTokenRevocationChecker;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppUserTokenVerifier implements AppUserAuthenticator {

    private static final String APP_INSTANCE_RESOURCE_TYPE = "AppInstance";
    private static final String APP_USE = "APP_USE";
    private static final String INVALID_TOKEN_MESSAGE = "The user token is invalid";
    private static final String FORBIDDEN_MESSAGE = "The signed-in user may not use this app instance";
    private static final String REJECTED_COUNTER_NAME = "bytechef_app_user_token_rejected";
    private static final String SCOPE_CLAIM = "scope";

    private static final Logger log = LoggerFactory.getLogger(AppUserTokenVerifier.class);

    private final ApplicationProperties applicationProperties;
    private final AuthorityService authorityService;
    private final ObjectProvider<JwtDecoder> jwtDecoderProvider;
    private final @Nullable MeterRegistry meterRegistry;
    private final PermissionService permissionService;
    private final ObjectProvider<SelfIssuedTokenRevocationChecker> revocationCheckerProvider;
    private final UserService userService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public AppUserTokenVerifier(
        ApplicationProperties applicationProperties, AuthorityService authorityService,
        ObjectProvider<JwtDecoder> jwtDecoderProvider, PermissionService permissionService,
        ObjectProvider<SelfIssuedTokenRevocationChecker> revocationCheckerProvider, UserService userService,
        ObjectProvider<MeterRegistry> meterRegistryProvider) {

        this.applicationProperties = applicationProperties;
        this.authorityService = authorityService;
        this.jwtDecoderProvider = jwtDecoderProvider;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.permissionService = permissionService;
        this.revocationCheckerProvider = revocationCheckerProvider;
        this.userService = userService;
    }

    @Override
    public AppUser authenticate(AppInstance appInstance, String tenantId, String userToken) {
        Jwt jwt = decode(userToken);

        checkClaims(jwt, appInstance, tenantId);

        String previousTenantId = TenantContext.getCurrentTenantId();

        TenantContext.setCurrentTenantId(tenantId);

        try {
            return authenticateUser(jwt, appInstance, tenantId);
        } finally {
            TenantContext.setCurrentTenantId(previousTenantId);
        }
    }

    public <T> T callAsUser(User user, Supplier<T> supplier) {
        List<GrantedAuthority> authorities = user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .flatMap(Optional::stream)
            .map(Authority::getName)
            .<GrantedAuthority>map(SimpleGrantedAuthority::new)
            .toList();

        return SecurityUtils.runAs(user.getLogin(), authorities, supplier);
    }

    public @Nullable String getIssuer() {
        String publicUrl = applicationProperties.getPublicUrl();

        if (StringUtils.isBlank(publicUrl)) {
            return null;
        }

        return StringUtils.removeEnd(publicUrl.strip(), "/");
    }

    private AppUser authenticateUser(Jwt jwt, AppInstance appInstance, String tenantId) {
        checkNotRevoked(jwt, tenantId);

        User user = userService.fetchUserByLogin(jwt.getSubject())
            .filter(User::isActivated)
            .orElseThrow(() -> forbidden("user_inactive"));

        Long appInstanceId = appInstance.getId();

        boolean allowed = callAsUser(
            user, () -> permissionService.hasResourceScope(appInstanceId, APP_INSTANCE_RESOURCE_TYPE, APP_USE));

        if (!allowed) {
            throw forbidden("forbidden");
        }

        return new AppUser(Objects.requireNonNull(user.getId()), user.getLogin());
    }

    private void checkClaims(Jwt jwt, AppInstance appInstance, String tenantId) {
        String issuer = getIssuer();

        if (issuer == null || !issuer.equals(jwt.getClaimAsString("iss"))) {
            throw invalidToken("issuer");
        }

        Instant expiresAt = jwt.getExpiresAt();

        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw invalidToken("expired");
        }

        if (!tenantId.equals(jwt.getClaimAsString(TENANT_ID_CLAIM))) {
            throw invalidToken("tenant");
        }

        String registeredClientId = appInstance.getRegisteredClientId();
        List<String> audiences = jwt.getAudience();

        if (registeredClientId == null) {
            throw invalidToken("client_unregistered");
        }

        if (audiences == null || !audiences.contains(registeredClientId)) {
            throw invalidToken("audience");
        }

        if (!hasAppUseScope(jwt)) {
            throw invalidToken("scope");
        }

        if (StringUtils.isBlank(jwt.getSubject())) {
            throw invalidToken("subject");
        }
    }

    private void checkNotRevoked(Jwt jwt, String tenantId) {
        SelfIssuedTokenRevocationChecker revocationChecker = revocationCheckerProvider.getIfAvailable();

        if (revocationChecker == null) {
            throw invalidToken("revocation_unavailable");
        }

        if (revocationChecker.isRevoked(jwt.getTokenValue(), tenantId)) {
            throw invalidToken("revoked");
        }
    }

    private Jwt decode(String userToken) {
        JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();

        if (jwtDecoder == null) {
            throw invalidToken("decoder_unavailable");
        }

        try {
            return jwtDecoder.decode(userToken);
        } catch (JwtValidationException jwtValidationException) {
            throw invalidToken("validation_failed");
        } catch (JwtException jwtException) {
            throw invalidToken("undecodable");
        }
    }

    private static boolean hasAppUseScope(Jwt jwt) {
        Object scope = jwt.getClaim(SCOPE_CLAIM);

        if (scope instanceof String scopeString) {
            return List.of(StringUtils.split(scopeString, ' '))
                .contains(APP_USE_SCOPE);
        }

        if (scope instanceof Collection<?> scopes) {
            return scopes.contains(APP_USE_SCOPE);
        }

        return false;
    }

    private AppUserAuthenticationException forbidden(String reason) {
        countRejection(reason);

        return new AppUserAuthenticationException(AppErrorType.APP_USER_FORBIDDEN, FORBIDDEN_MESSAGE);
    }

    private AppUserAuthenticationException invalidToken(String reason) {
        countRejection(reason);

        return new AppUserAuthenticationException(AppErrorType.APP_USER_TOKEN_INVALID, INVALID_TOKEN_MESSAGE);
    }

    private void countRejection(String reason) {
        if (meterRegistry == null) {
            return;
        }

        try {
            Counter.builder(REJECTED_COUNTER_NAME)
                .tag("reason", reason)
                .description("User tokens rejected by the app user token verifier, tagged by reason")
                .register(meterRegistry)
                .increment();
        } catch (RuntimeException runtimeException) {
            log.warn(
                "The rejected user token counter could not be incremented ({})", runtimeException.getClass()
                    .getName());
        }
    }
}
