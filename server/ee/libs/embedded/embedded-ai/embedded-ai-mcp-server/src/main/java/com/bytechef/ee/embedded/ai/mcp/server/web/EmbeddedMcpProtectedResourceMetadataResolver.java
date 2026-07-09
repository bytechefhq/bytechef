/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver.Surface;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the RFC 9728 protected-resource metadata for an embedded MCP resource. The resource's path secret is a
 * {@link TenantKey}, so the tenant is derived from the resource URL (no database) and, within that tenant, the identity
 * providers flagged for embedded MCP (resolved via the shared {@link McpTenantIssuerResolver}, surface
 * {@link Surface#EMBEDDED}, so discovery advertises exactly the issuers embedded trust accepts) are advertised as the
 * endpoint's authorization servers - the "your IdP runs the show" path, where a generic MCP client authenticates
 * directly against the tenant's IdP.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpProtectedResourceMetadataResolver {

    private static final Pattern EMBEDDED_MCP_RESOURCE_PATTERN = Pattern.compile("/api/embedded/(.+)/mcp");

    private final McpTenantIssuerResolver mcpTenantIssuerResolver;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpProtectedResourceMetadataResolver(McpTenantIssuerResolver mcpTenantIssuerResolver) {
        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
    }

    public Optional<ProtectedResourceMetadata> resolveMetadata(String resource) {
        Matcher matcher = EMBEDDED_MCP_RESOURCE_PATTERN.matcher(resource);

        if (!matcher.find()) {
            return Optional.empty();
        }

        String tenantId;

        try {
            tenantId = TenantKey.parse(matcher.group(1))
                .getTenantId();
        } catch (RuntimeException runtimeException) {
            return Optional.empty();
        }

        List<String> authorizationServers = TenantContext
            .callWithTenantId(tenantId, () -> mcpTenantIssuerResolver.resolve(Surface.EMBEDDED))
            .stream()
            .map(McpTenantIssuer::issuerUri)
            .toList();

        if (authorizationServers.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ProtectedResourceMetadata(resource, authorizationServers));
    }
}
