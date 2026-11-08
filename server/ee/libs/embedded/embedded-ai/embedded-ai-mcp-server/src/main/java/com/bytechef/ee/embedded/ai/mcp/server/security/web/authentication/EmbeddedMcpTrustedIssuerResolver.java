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
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpTrustedIssuerResolver {

    private final McpTenantIssuerResolver mcpTenantIssuerResolver;

    @SuppressFBWarnings("EI2")
    public EmbeddedMcpTrustedIssuerResolver(McpTenantIssuerResolver mcpTenantIssuerResolver) {
        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
    }

    public Set<String> resolveTrustedIssuerUris() {
        return mcpTenantIssuerResolver.resolve(Surface.EMBEDDED)
            .stream()
            .map(McpTenantIssuer::issuerUri)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    public McpTenantIssuer resolveTenantIssuer(String issuerUri) {
        return mcpTenantIssuerResolver.resolveIssuer(Surface.EMBEDDED, issuerUri);
    }
}
