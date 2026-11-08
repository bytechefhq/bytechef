/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver.Surface;
import com.bytechef.platform.security.web.config.McpResourceServerProperties;
import com.bytechef.platform.security.web.config.McpResourceServerProperties.Issuer;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the RFC 9728 protected-resource metadata for an automation or management MCP endpoint. The resource's path
 * secret is a {@link TenantKey}, so the tenant is derived from the resource URL (no database) and, within that tenant,
 * the external identity providers flagged for the endpoint's surface are advertised as the endpoint's authorization
 * servers alongside ByteChef's own configured issuers - the "your IdP runs the show" discovery path, where a generic
 * MCP client can authenticate directly against the tenant's IdP.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class McpTenantProtectedResourceMetadataResolver {

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("/api/(automation|management)/(.+)/mcp");

    private final McpResourceServerProperties mcpResourceServerProperties;
    private final McpTenantIssuerResolver mcpTenantIssuerResolver;

    @SuppressFBWarnings("EI2")
    public McpTenantProtectedResourceMetadataResolver(
        McpResourceServerProperties mcpResourceServerProperties, McpTenantIssuerResolver mcpTenantIssuerResolver) {

        this.mcpResourceServerProperties = mcpResourceServerProperties;
        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
    }

    public Optional<McpProtectedResourceMetadata> resolveMetadata(String resource) {
        Matcher matcher = RESOURCE_PATTERN.matcher(resource);

        if (!matcher.find()) {
            return Optional.empty();
        }

        Surface surface = "automation".equals(matcher.group(1)) ? Surface.AUTOMATION : Surface.MANAGEMENT;

        String tenantId;

        try {
            tenantId = TenantKey.parse(matcher.group(2))
                .getTenantId();
        } catch (RuntimeException runtimeException) {
            return Optional.empty();
        }

        // Insertion-ordered so the tenant's own identity providers are advertised ahead of ByteChef's issuers.
        Set<String> authorizationServers = new LinkedHashSet<>();

        TenantContext.callWithTenantId(tenantId, () -> mcpTenantIssuerResolver.resolve(surface))
            .forEach(mcpTenantIssuer -> authorizationServers.add(mcpTenantIssuer.issuerUri()));

        mcpResourceServerProperties.getIssuers()
            .stream()
            .map(Issuer::getUri)
            .filter(Objects::nonNull)
            .forEach(authorizationServers::add);

        if (authorizationServers.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new McpProtectedResourceMetadata(resource, List.copyOf(authorizationServers)));
    }
}
