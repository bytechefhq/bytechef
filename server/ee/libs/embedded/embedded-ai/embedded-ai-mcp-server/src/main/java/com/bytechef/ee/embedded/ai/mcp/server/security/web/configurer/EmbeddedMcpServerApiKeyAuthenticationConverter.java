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
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Converts an embedded MCP request into an {@link EmbeddedMcpServerApiKeyAuthenticationToken}. The signing-key JWT is
 * no longer mandatory at this layer: a missing {@code Authorization} header yields a token whose external user id is
 * {@code null} (tenant derived from the endpoint's path secret) instead of a {@code BadCredentialsException}, so the
 * per-server provider can serve a token-less request anonymously when the target server does not require
 * authentication. The provider resolves the target MCP server from the path secret carried on the token and decides
 * whether a token is actually required.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerApiKeyAuthenticationConverter extends AbstractApiKeyAuthenticationConverter {

    private static final Pattern PATH_PATTERN = Pattern.compile("^/api/embedded/(.+)/(mcp|sse|message)");

    private final SigningKeyService signingKeyService;

    EmbeddedMcpServerApiKeyAuthenticationConverter(SigningKeyService signingKeyService) {
        this.signingKeyService = signingKeyService;
    }

    @Override
    @Nullable
    public Authentication convert(HttpServletRequest request) {
        Authentication existingAuthentication = SecurityContextHolder.getContext()
            .getAuthentication();

        // A preceding credential filter (the OAuth2 filter) already authenticated this request; do not reprocess it as
        // a signing-key token. This makes the signing-key filter a genuine fallback rather than a second credential
        // check that would reject an already-accepted OAuth2 token.
        if (existingAuthentication != null && existingAuthentication.isAuthenticated()) {
            return null;
        }

        String authToken = fetchAuthToken(request);

        Environment environment = getEnvironment(request);

        String mcpServerSecretKey = extractMcpServerSecretKey(request);

        if (authToken == null) {
            return new EmbeddedMcpServerApiKeyAuthenticationToken(
                environment.ordinal(), null, fetchTenantId(mcpServerSecretKey), mcpServerSecretKey);
        }

        try {
            Jws<Claims> jws = getJws(authToken, environment.ordinal());

            Claims payload = jws.getPayload();

            String externalUserId = payload.getSubject();

            JwsHeader header = jws.getHeader();

            TenantKey tenantKey = TenantKey.parse(header.getKeyId());

            return new EmbeddedMcpServerApiKeyAuthenticationToken(
                environment.ordinal(), externalUserId, tenantKey.getTenantId(), mcpServerSecretKey);
        } catch (Exception exception) {
            throw new BadCredentialsException("Invalid embedded MCP signing-key token", exception);
        }
    }

    @Nullable
    private static String extractMcpServerSecretKey(HttpServletRequest request) {
        Matcher matcher = PATH_PATTERN.matcher(request.getServletPath());

        if (!matcher.matches()) {
            return null;
        }

        return matcher.group(1);
    }

    @Nullable
    private static String fetchTenantId(@Nullable String mcpServerSecretKey) {
        if (mcpServerSecretKey == null) {
            return null;
        }

        try {
            TenantKey tenantKey = TenantKey.parse(mcpServerSecretKey);

            return tenantKey.getTenantId();
        } catch (Exception exception) {
            return null;
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
