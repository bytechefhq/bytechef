/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.filter;

import com.bytechef.platform.user.domain.IdentityProvider;
import com.bytechef.platform.user.service.IdentityProviderService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;

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

        String matchedTenantId = findTenantIdByScimApiKey(bearerToken);

        if (matchedTenantId == null) {
            sendUnauthorized(response, "Invalid SCIM API key");

            return;
        }

        TenantContext.runWithTenantId(matchedTenantId, () -> filterChain.doFilter(request, response));
    }

    private String findTenantIdByScimApiKey(String apiKey) {
        List<String> tenantIds = tenantService.getTenantIds();

        for (String tenantId : tenantIds) {
            Optional<IdentityProvider> identityProvider = TenantContext.callWithTenantId(
                tenantId, () -> identityProviderService.fetchByScimApiKey(apiKey));

            if (identityProvider.isPresent()) {
                return tenantId;
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
}
