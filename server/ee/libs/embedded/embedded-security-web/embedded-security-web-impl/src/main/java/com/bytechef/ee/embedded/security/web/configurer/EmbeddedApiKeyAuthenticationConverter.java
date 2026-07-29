/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

import com.bytechef.ee.embedded.connected.user.constant.ConnectedUserConstants;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.ee.embedded.security.web.authentication.EmbeddedApiKeyAuthenticationToken;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.web.filter.AbstractApiKeyAuthenticationConverter;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.Nullable;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.security.PublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * Authentication converter for embedded API key authentication.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    /**
     * The reserved-segment allowlist itself now lives on {@link ConnectedUserConstants#FRONTEND_RESERVED_PATH_SEGMENTS}
     * (embedded-connected-user-api) so {@code ConnectedUserServiceImpl} can share it and reject a connected user's
     * externalId that collides with a reserved word at creation time -- closing the collision at the source instead of
     * only rejecting it here.
     *
     * <p>
     * Those routes are JWT-only by design -- the caller's externalUserId comes from the JWT {@code sub} claim, never
     * from the URL. {@link #EXTERNAL_USER_ID_PATTERN} cannot tell a Frontend route's literal resource segment apart
     * from a genuine {@code {externalUserId}} path segment (both are just "the first path component after
     * {@code /v<n>/}"), so a non-JWT bearer token hitting one of these routes would otherwise mint a phantom
     * {@code ConnectedUser} named after the literal segment (e.g. {@code "automation"}, {@code "me"}) via
     * {@code EmbeddedApiKeyAuthenticationProvider}'s get-or-create.
     *
     * <p>
     * {@code EmbeddedApiKeyAuthenticationConverterTest} exercises {@link #convert(HttpServletRequest)} for every entry
     * in the allowlist, so a change here that stops rejecting a known Frontend route is caught immediately. When adding
     * a NEW no-{@code {externalUserId}} route under {@code /api/embedded/v<n>/} in any module, add its literal first
     * path segment to {@code ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS} and a matching regression test.
     */
    static final Pattern EXTERNAL_USER_ID_PATTERN = Pattern.compile(".*/v\\d+/([^/]+)/.*");
    static final Pattern JWT_TOKEN_PATTERN =
        Pattern.compile("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$");

    private final JwtTokenService jwtTokenService;
    private final SigningKeyService signingKeyService;

    EmbeddedApiKeyAuthenticationConverter(JwtTokenService jwtTokenService, SigningKeyService signingKeyService) {
        this.jwtTokenService = jwtTokenService;
        this.signingKeyService = signingKeyService;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String authToken = fetchAuthToken(request);

        if (authToken == null || authToken.isBlank()) {
            return null;
        }

        Environment environment = getEnvironment(request);
        Matcher jwtTokenMatcher = JWT_TOKEN_PATTERN.matcher(authToken);

        if (jwtTokenMatcher.find()) {
            Jws<Claims> jws = getJws(authToken, environment.ordinal());

            Claims payload = jws.getPayload();

            String externalUserId = payload.getSubject();

            JwsHeader header = jws.getHeader();

            TenantKey tenantKey = TenantKey.parse(header.getKeyId());

            return new EmbeddedApiKeyAuthenticationToken(
                environment.ordinal(), externalUserId, null, tenantKey.getTenantId());
        } else {
            String externalUserId;
            Matcher matcher = EXTERNAL_USER_ID_PATTERN.matcher(request.getRequestURI());

            if (matcher.matches()) {
                externalUserId = matcher.group(1);
            } else {
                throw new IllegalArgumentException("externalUserId parameter is required");
            }

            if (ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS.contains(externalUserId)) {
                // Frontend routes are JWT-only by design (see ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS).
                // A non-JWT bearer token landing here means EXTERNAL_USER_ID_PATTERN merely captured a Frontend
                // route's literal top-level resource segment -- reject instead of authenticating as a phantom
                // connected user.
                throw new BadCredentialsException("Non-JWT tokens are not accepted on this endpoint");
            }

            TenantKey tenantKey = TenantKey.parse(authToken);

            return new EmbeddedApiKeyAuthenticationToken(
                environment.ordinal(), externalUserId, authToken, tenantKey.getTenantId());
        }
    }

    private Jws<Claims> getJws(String secretKey, long environmentId) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator(environmentId, jwtTokenService, signingKeyService))
            .build()
            .parseSignedClaims(secretKey);
    }

    private record SigningKeyLocator(
        long environmentId, JwtTokenService jwtTokenService, SigningKeyService signingKeyService)
        implements Locator<Key> {

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            PublicKey publicKey = jwtTokenService.getPublicKey(keyId);

            if (publicKey != null) {
                return publicKey;
            }

            TenantKey tenantKey = TenantKey.parse(keyId);

            return TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> signingKeyService.getPublicKey(keyId, environmentId));
        }
    }
}
