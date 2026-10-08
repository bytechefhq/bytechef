/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.security.web.configurer;

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
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.util.UriUtils;

/**
 * Authentication converter for embedded API key authentication.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    static final Pattern API_KEY_PATH_PATTERN = Pattern.compile(
        "^/api/embedded/v\\d+/(?:external/([^/]+)/integration-instances(?:/.*)?|([^/]+)(?:/.*)?)$");
    static final Pattern CONNECTED_USER_ONLY_PATH_PATTERN =
        Pattern.compile("^/api/embedded/v\\d+/(?:app-events|workflows/[^/]+)/?$");
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

            Integer claimedEnvironmentId = payload.get("environmentId", Integer.class);
            long environmentId = environment.ordinal();

            if (claimedEnvironmentId != null) {
                boolean builderToken = jwtTokenService.getPublicKey(header.getKeyId()) != null;
                String environmentHeader = request.getHeader("X-ENVIRONMENT");

                if ((!builderToken || StringUtils.isNotBlank(environmentHeader)) &&
                    environment.ordinal() != claimedEnvironmentId) {

                    throw new BadCredentialsException("X-ENVIRONMENT does not match the token's environment");
                }

                environmentId = claimedEnvironmentId;
            }

            return new EmbeddedApiKeyAuthenticationToken(
                environmentId, externalUserId, null, tenantKey.getTenantId());
        } else {
            String contextPath = StringUtils.defaultString(request.getContextPath());
            String requestURI = request.getRequestURI();

            String path = requestURI.substring(contextPath.length());

            Matcher connectedUserOnlyPathMatcher = CONNECTED_USER_ONLY_PATH_PATTERN.matcher(path);

            if (connectedUserOnlyPathMatcher.matches()) {
                throw new BadCredentialsException("This endpoint requires a connected-user token");
            }

            Matcher matcher = API_KEY_PATH_PATTERN.matcher(path);

            if (!matcher.matches()) {
                throw new BadCredentialsException("An API key requires the external user id as the first path segment");
            }

            String externalMcpInstanceUserId = matcher.group(1);

            String externalUserId;

            try {
                externalUserId = UriUtils.decode(
                    externalMcpInstanceUserId == null ? matcher.group(2) : externalMcpInstanceUserId,
                    StandardCharsets.UTF_8);
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new BadCredentialsException("The external user id is not validly encoded",
                    illegalArgumentException);
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
