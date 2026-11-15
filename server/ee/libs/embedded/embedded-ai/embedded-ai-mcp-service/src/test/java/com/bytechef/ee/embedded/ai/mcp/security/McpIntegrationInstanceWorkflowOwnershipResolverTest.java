/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceWorkflowRepository;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceWorkflowOwnershipResolverTest {

    private final McpIntegrationInstanceWorkflowRepository mcpIntegrationInstanceWorkflowRepository =
        mock(McpIntegrationInstanceWorkflowRepository.class);
    private final McpIntegrationInstanceWorkflowOwnershipResolver mcpIntegrationInstanceWorkflowOwnershipResolver =
        new McpIntegrationInstanceWorkflowOwnershipResolver(mcpIntegrationInstanceWorkflowRepository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(mcpIntegrationInstanceWorkflowOwnershipResolver.resourceType())
            .isEqualTo("McpIntegrationInstanceWorkflow");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        when(mcpIntegrationInstanceWorkflowRepository.existsById(4L)).thenReturn(true);

        assertThat(mcpIntegrationInstanceWorkflowOwnershipResolver.resolveOwner(4L))
            .isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(mcpIntegrationInstanceWorkflowOwnershipResolver.resolveOwner(Long.valueOf(4L)))
            .isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        assertThat(mcpIntegrationInstanceWorkflowOwnershipResolver.resolveOwner(4L))
            .isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANonNumericIdIsUnknown() {
        assertThat(mcpIntegrationInstanceWorkflowOwnershipResolver.resolveOwner("not-a-number"))
            .isEqualTo(ResourceOwner.unknown());
    }
}
