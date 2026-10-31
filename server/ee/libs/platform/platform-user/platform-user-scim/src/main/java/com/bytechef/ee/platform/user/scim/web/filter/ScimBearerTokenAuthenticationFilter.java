/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.filter;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Filter that authenticates SCIM 2.0 requests using Bearer token authentication. The Bearer token is matched against
 * the {@code scimApiKey} field of configured identity providers. When a match is found, the tenant context is set for
 * the duration of the request.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ScimBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int PRINCIPAL_MAX_LENGTH = 50;
    private static final String PRINCIPAL_PREFIX = "scim:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SCIM_PATH_PREFIX = "/api/scim/v2/";

    private final IdentityProviderService identityProviderService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public ScimBearerTokenAuthenticationFilter(
        IdentityProviderService identityProviderService, TenantService tenantService) {

        this.identityProviderService = identityProviderService;
        this.tenantService = tenantService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI()
            .startsWith(SCIM_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            sendUnauthorized(response, "Missing or invalid Authorization header");

            return;
        }

        String bearerToken = authorizationHeader.substring(BEARER_PREFIX.length())
            .trim();

        if (bearerToken.isEmpty()) {
            sendUnauthorized(response, "Empty Bearer token");

            return;
        }

        ScimClient scimClient = findScimClientByScimApiKey(bearerToken);

        if (scimClient == null) {
            sendUnauthorized(response, "Invalid SCIM API key");

            return;
        }

        TenantContext.runWithTenantId(
            scimClient.tenantId(),
            () -> doFilterAsScimClient(request, response, filterChain, scimClient.principal()));
    }

    private static String toPrincipal(IdentityProvider identityProvider) {
        String suffix = " (" + identityProvider.getId() + ")";
        String name = Objects.requireNonNullElse(identityProvider.getName(), "");

        int maxNameLength = PRINCIPAL_MAX_LENGTH - PRINCIPAL_PREFIX.length() - suffix.length();

        if (name.length() > maxNameLength) {
            name = name.substring(0, Math.max(0, maxNameLength));
        }

        return PRINCIPAL_PREFIX + name + suffix;
    }

    private void doFilterAsScimClient(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String principal)
        throws IOException, ServletException {

        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

        SecurityContext previousSecurityContext = securityContextHolderStrategy.getContext();
        SecurityContext securityContext = securityContextHolderStrategy.createEmptyContext();

        securityContext.setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()));

        securityContextHolderStrategy.setContext(securityContext);

        try {
            filterChain.doFilter(request, response);
        } finally {
            securityContextHolderStrategy.setContext(previousSecurityContext);
        }
    }

    private @Nullable ScimClient findScimClientByScimApiKey(String apiKey) {
        List<String> tenantIds = tenantService.getTenantIds();

        for (String tenantId : tenantIds) {
            Optional<IdentityProvider> identityProvider = TenantContext.callWithTenantId(
                tenantId, () -> identityProviderService.fetchByScimApiKey(apiKey));

            if (identityProvider.isPresent()) {
                return new ScimClient(tenantId, toPrincipal(identityProvider.get()));
            }
        }

        return null;
    }

    @SuppressFBWarnings("XSS_SERVLET")
    private void sendUnauthorized(HttpServletResponse response, String detail) throws IOException {
        ObjectNode errorNode = OBJECT_MAPPER.createObjectNode();

        ArrayNode schemasNode = errorNode.putArray("schemas");

        schemasNode.add("urn:ietf:params:scim:api:messages:2.0:Error");

        errorNode.put("detail", detail);
        errorNode.put("status", "401");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/scim+json");
        response.getWriter()
            .write(OBJECT_MAPPER.writeValueAsString(errorNode));
    }

    private record ScimClient(String tenantId, String principal) {
    }
}
