/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.security.web.authentication;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver.Surface;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the OAuth2 issuers trusted on the embedded MCP endpoint for the current tenant: the tenant's identity
 * providers flagged for embedded MCP (delegated to the shared {@link McpTenantIssuerResolver}, surface
 * {@link Surface#EMBEDDED}, so the same per-tenant trust, caching, and group-to-authority mapping applies as on the
 * automation/management endpoints) plus ByteChef's own embedded authorization server. Must be called within the tenant
 * established from the endpoint's path secret, so a token trusted for one tenant does not authenticate on another's
 * endpoint.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpTrustedIssuerResolver {

    private final @Nullable String embeddedAuthorizationServerIssuerUri;
    private final McpTenantIssuerResolver mcpTenantIssuerResolver;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpTrustedIssuerResolver(
        McpTenantIssuerResolver mcpTenantIssuerResolver, @Nullable String embeddedAuthorizationServerIssuerUri) {

        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
        this.embeddedAuthorizationServerIssuerUri = embeddedAuthorizationServerIssuerUri;
    }

    public Set<String> resolveTrustedIssuerUris() {
        Set<String> trustedIssuerUris = mcpTenantIssuerResolver.resolve(Surface.EMBEDDED)
            .stream()
            .map(McpTenantIssuer::issuerUri)
            .collect(Collectors.toCollection(HashSet::new));

        if (embeddedAuthorizationServerIssuerUri != null && !embeddedAuthorizationServerIssuerUri.isBlank()) {
            trustedIssuerUris.add(embeddedAuthorizationServerIssuerUri);
        }

        return Set.copyOf(trustedIssuerUris);
    }

    /**
     * Whether the tenant's embedded identity provider for the given issuer requires MCP audience validation - i.e. an
     * accepted token must carry this endpoint's URL as its {@code aud}. Must be called within the tenant established
     * from the endpoint's path secret.
     */
    public boolean isAudienceValidationRequired(String issuerUri) {
        McpTenantIssuer mcpTenantIssuer = mcpTenantIssuerResolver.resolveIssuer(Surface.EMBEDDED, issuerUri);

        return mcpTenantIssuer != null && mcpTenantIssuer.validateAudience();
    }

    /**
     * The tenant's embedded identity provider for the given issuer (its group claim, group-to-authority map, and
     * audience policy), or {@code null} if the current tenant does not trust it for embedded MCP. Used to map the
     * token's group claim to ByteChef authorities for per-component tool authorization. Must be called within the
     * tenant established from the endpoint's path secret.
     */
    @Nullable
    public McpTenantIssuer resolveTenantIssuer(String issuerUri) {
        return mcpTenantIssuerResolver.resolveIssuer(Surface.EMBEDDED, issuerUri);
    }
}
