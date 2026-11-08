/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.web;

import com.bytechef.ee.platform.security.web.mcp.oauth2.McpProtectedResourceMetadata;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantIssuerResolver.Surface;
import com.bytechef.ee.platform.security.web.mcp.oauth2.McpTenantSecretVerifier;
import com.bytechef.platform.security.web.mcp.oauth2.McpProtectedResourceUrls;
import com.bytechef.platform.security.web.mcp.oauth2.McpTenantIssuer;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpProtectedResourceMetadataResolver {

    private static final Pattern EMBEDDED_MCP_RESOURCE_PATTERN = Pattern.compile("/api/embedded/(.+)/mcp");
    private static final String RESOURCE_NAME = "ByteChef";

    private final McpTenantIssuerResolver mcpTenantIssuerResolver;
    private final McpTenantSecretVerifier mcpTenantSecretVerifier;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpProtectedResourceMetadataResolver(
        McpTenantIssuerResolver mcpTenantIssuerResolver, McpTenantSecretVerifier mcpTenantSecretVerifier) {

        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
        this.mcpTenantSecretVerifier = mcpTenantSecretVerifier;
    }

    public Optional<McpProtectedResourceMetadata> resolveMetadata(String resource, HttpServletRequest request) {
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

        if (!mcpTenantSecretVerifier.isVerified(request, tenantId, matcher.group(0))) {
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

        return Optional.of(
            new McpProtectedResourceMetadata(
                resource, authorizationServers, List.of(McpProtectedResourceUrls.EMBEDDED_SCOPE), RESOURCE_NAME,
                false));
    }
}
