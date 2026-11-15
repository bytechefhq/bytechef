/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.filter;

import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.domain.ScimApiKey;
import com.bytechef.ee.platform.user.scim.ScimConstants;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Authenticates SCIM requests by the identity provider's bearer token. The token names its tenant, so the request is
 * resolved with one lookup in that tenant; the identity provider then becomes the request's principal, holding only the
 * {@code ROLE_SCIM} authority, and the request runs in the tenant.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ScimBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int PRINCIPAL_MAX_LENGTH = 50;
    private static final String PRINCIPAL_PREFIX = "scim:";

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
        String path = request.getRequestURI()
            .substring(request.getContextPath()
                .length());

        return !path.startsWith(ScimConstants.PATH_PREFIX);
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

        Optional<String> tenantId = ScimApiKey.tenantIdOf(bearerToken);

        if (tenantId.isEmpty() || !isTenantOfThisMode(tenantId.get())) {
            sendUnauthorized(response, "Invalid SCIM API key");

            return;
        }

        String previousTenantId = TenantContext.getCurrentTenantId();
        SecurityContext previousSecurityContext = SecurityContextHolder.getContext();

        TenantContext.setCurrentTenantId(tenantId.get());

        try {
            Optional<IdentityProvider> identityProvider = identityProviderService.fetchByScimApiKey(bearerToken);

            if (identityProvider.isEmpty()) {
                sendUnauthorized(response, "Invalid SCIM API key");

                return;
            }

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(
                new PreAuthenticatedAuthenticationToken(
                    toPrincipal(identityProvider.get()), null,
                    List.of(new SimpleGrantedAuthority(ScimConstants.SCIM_AUTHORITY))));

            SecurityContextHolder.setContext(securityContext);

            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.setContext(previousSecurityContext);
            TenantContext.setCurrentTenantId(previousTenantId);
        }
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

    private boolean isTenantOfThisMode(String tenantId) {
        return tenantService.isMultiTenantEnabled() != TenantContext.DEFAULT_TENANT_ID.equals(tenantId);
    }

    @SuppressFBWarnings("XSS_SERVLET")
    private void sendUnauthorized(HttpServletResponse response, String detail) throws IOException {
        ObjectNode errorNode = OBJECT_MAPPER.createObjectNode();

        ArrayNode schemasNode = errorNode.putArray("schemas");

        schemasNode.add(ScimConstants.ERROR_SCHEMA);

        errorNode.put("detail", detail);
        errorNode.put("status", "401");

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/scim+json");
        response.getWriter()
            .write(OBJECT_MAPPER.writeValueAsString(errorNode));
    }
}
