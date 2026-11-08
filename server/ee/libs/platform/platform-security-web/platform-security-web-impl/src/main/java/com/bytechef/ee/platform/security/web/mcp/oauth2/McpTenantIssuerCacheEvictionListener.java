/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import com.bytechef.ee.platform.user.event.IdentityProviderChangedEvent;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Evicts the {@link McpTenantIssuerResolver} cache whenever an identity provider is created, updated, or deleted, so
 * the automation/management and embedded MCP trust and discovery reflect the change immediately rather than after the
 * cache TTL.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class McpTenantIssuerCacheEvictionListener {

    private final McpTenantIssuerResolver mcpTenantIssuerResolver;

    @SuppressFBWarnings("EI2")
    public McpTenantIssuerCacheEvictionListener(McpTenantIssuerResolver mcpTenantIssuerResolver) {
        this.mcpTenantIssuerResolver = mcpTenantIssuerResolver;
    }

    @EventListener
    public void onIdentityProviderChanged(IdentityProviderChangedEvent identityProviderChangedEvent) {
        mcpTenantIssuerResolver.evictAll();
    }
}
