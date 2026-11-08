/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.security.web.mcp.oauth2;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.ee.platform.user.event.IdentityProviderChangedEvent;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpTenantIssuerCacheEvictionListenerTest {

    private final McpTenantIssuerResolver mcpTenantIssuerResolver = mock(McpTenantIssuerResolver.class);
    private final McpTenantIssuerCacheEvictionListener listener =
        new McpTenantIssuerCacheEvictionListener(mcpTenantIssuerResolver);

    @Test
    void testEvictsOnIdentityProviderChanged() {
        listener.onIdentityProviderChanged(new IdentityProviderChangedEvent());

        verify(mcpTenantIssuerResolver).evictAll();
    }
}
