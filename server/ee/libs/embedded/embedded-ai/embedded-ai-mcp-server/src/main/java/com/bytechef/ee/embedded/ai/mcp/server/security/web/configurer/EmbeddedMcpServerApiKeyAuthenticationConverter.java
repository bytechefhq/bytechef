/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer;

import com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication.EmbeddedMcpServerApiKeyAuthenticationToken;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    private static final Pattern SECRET_KEY_PATH_PATTERN = Pattern.compile("^/api/embedded/(.+)/mcp");

    private final SigningKeyService signingKeyService;

    EmbeddedMcpServerApiKeyAuthenticationConverter(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        String mcpServerSecretKey = getMcpServerSecretKey(request);
        Environment environment = getEnvironment(request);

        Jws<Claims> jws = fetchJws(fetchAuthToken(request), environment.ordinal());

        if (jws == null) {
            return new EmbeddedMcpServerApiKeyAuthenticationToken(
                environment.ordinal(), null, getTenantId(mcpServerSecretKey), mcpServerSecretKey);
        }

        Claims payload = jws.getPayload();

        String externalUserId = payload.getSubject();

        JwsHeader header = jws.getHeader();

        TenantKey tenantKey = TenantKey.parse(header.getKeyId());

        return new EmbeddedMcpServerApiKeyAuthenticationToken(
            environment.ordinal(), externalUserId, tenantKey.getTenantId(), mcpServerSecretKey);
    }

    @Nullable
    private Jws<Claims> fetchJws(@Nullable String authToken, long environmentId) {
        if (authToken == null) {
            return null;
        }

        try {
            return getJws(authToken, environmentId);
        } catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private static String getMcpServerSecretKey(HttpServletRequest request) {
        Matcher matcher = SECRET_KEY_PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            throw new BadCredentialsException("Invalid MCP server secret key");
        }

        return matcher.group(1);
    }

    private static String getTenantId(String mcpServerSecretKey) {
        try {
            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return tenantKey.getTenantId();
        } catch (RuntimeException runtimeException) {
            throw new BadCredentialsException("Invalid MCP server secret key", runtimeException);
        }
    }

    private Jws<Claims> getJws(String secretKey, long environmentId) {
        return Jwts.parser()
            .keyLocator(new SigningKeyLocator(environmentId, signingKeyService))
            .build()
            .parseSignedClaims(secretKey);
    }

    private record SigningKeyLocator(long environmentId, SigningKeyService signingKeyService)
        implements Locator<Key> {

        @Override
        public Key locate(Header header) {
            String keyId = (String) header.get("kid");

            TenantKey tenantKey = TenantKey.parse(keyId);

            return TenantContext.callWithTenantId(
                tenantKey.getTenantId(), () -> signingKeyService.getPublicKey(keyId, environmentId));
        }
    }
}
