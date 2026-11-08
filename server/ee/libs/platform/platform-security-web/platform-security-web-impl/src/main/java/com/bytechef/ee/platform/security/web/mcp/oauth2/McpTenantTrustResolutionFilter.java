/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver.Surface;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantTrustContext;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @version ee
 * @author Ivica Cardic
 */
public class McpTenantTrustResolutionFilter extends OncePerRequestFilter {

    private static final Pattern AUTOMATION_SECRET_PATTERN = Pattern.compile("/api/automation/(.+)/mcp");
    private static final Pattern MANAGEMENT_SECRET_PATTERN = Pattern.compile("/api/management/(.+)/mcp");

    private final McpTenantIssuerResolver mcpTenantIssuerResolver;
    private final McpTenantSecretVerifier mcpTenantSecretVerifier;

    @SuppressFBWarnings("EI2")
    public McpTenantTrustResolutionFilter(
        McpTenantIssuerResolver mcpTenantIssuerResolver, McpTenantSecretVerifier mcpTenantSecretVerifier) {

        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
        this.mcpTenantSecretVerifier = mcpTenantSecretVerifier;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        boolean populated = populateTrustContext(request);

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (populated) {
                McpTenantTrustContext.clear();
            }
        }
    }

    private boolean populateTrustContext(HttpServletRequest request) {
        if (!isBearerJwt(request)) {
            return false;
        }

        String requestUri = request.getRequestURI();

        Surface surface;
        Matcher matcher;

        Matcher automationMatcher = AUTOMATION_SECRET_PATTERN.matcher(requestUri);
        Matcher managementMatcher = MANAGEMENT_SECRET_PATTERN.matcher(requestUri);

        if (automationMatcher.find()) {
            surface = Surface.AUTOMATION;
            matcher = automationMatcher;
        } else if (managementMatcher.find()) {
            surface = Surface.MANAGEMENT;
            matcher = managementMatcher;
        } else {
            return false;
        }

        String tenantId;

        try {
            tenantId = TenantKey.parse(matcher.group(1))
                .getTenantId();
        } catch (RuntimeException runtimeException) {
            return false;
        }

        if (!mcpTenantSecretVerifier.isVerified(request, tenantId, request.getServletPath())) {
            return false;
        }

        List<McpTenantIssuer> tenantIssuers = TenantContext.callWithTenantId(
            tenantId, () -> mcpTenantIssuerResolver.resolve(surface));

        McpTenantTrustContext.set(tenantIssuers);

        return true;
    }

    private static boolean isBearerJwt(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return false;
        }

        String token = authorization.substring(7);

        return token.indexOf('.') >= 0;
    }
}
