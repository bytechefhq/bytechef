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
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.PublicKey;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;

/**
 * Authentication converter for embedded API key authentication.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    static final Pattern EXTERNAL_USER_ID_PATTERN = Pattern.compile(".*/v\\d+/([^/]+)/.*");
    static final Pattern PUBLIC_API_EXTERNAL_USER_ID_PATTERN = Pattern.compile("^/api/embedded/v\\d+/([^/]+)(?:/.*)?$");
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

            String pathExternalUserId = fetchPathExternalUserId(request);

            if (pathExternalUserId != null && !pathExternalUserId.equals(externalUserId)) {
                throw new BadCredentialsException("Token subject does not match the external user in the request path");
            }

            verifyEnvironmentClaim(payload, environment);

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
                throw new BadCredentialsException("Non-JWT tokens are not accepted on this endpoint");
            }

            TenantKey tenantKey = TenantKey.parse(authToken);

            return new EmbeddedApiKeyAuthenticationToken(
                environment.ordinal(), externalUserId, authToken, tenantKey.getTenantId());
        }
    }

    @Nullable
    private static String fetchPathExternalUserId(HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && requestPath.startsWith(contextPath)) {
            requestPath = requestPath.substring(contextPath.length());
        }

        Matcher matcher = PUBLIC_API_EXTERNAL_USER_ID_PATTERN.matcher(requestPath);

        if (!matcher.matches()) {
            return null;
        }

        String pathExternalUserId = StringUtils.uriDecode(matcher.group(1), StandardCharsets.UTF_8);

        if (ConnectedUserConstants.FRONTEND_RESERVED_PATH_SEGMENTS.contains(pathExternalUserId)) {
            return null;
        }

        return pathExternalUserId;
    }

    private static void verifyEnvironmentClaim(Claims payload, Environment environment) {
        if (!payload.containsKey(JwtTokenService.ENVIRONMENT_ID_CLAIM)) {
            return;
        }

        Object environmentIdClaim = payload.get(JwtTokenService.ENVIRONMENT_ID_CLAIM);

        if (!(environmentIdClaim instanceof Number environmentId) ||
            environmentId.longValue() != environment.ordinal()) {

            throw new BadCredentialsException("Token environment does not match the request environment");
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
