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
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceToolRepository;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceOwnershipResolversTest {

    private final McpIntegrationInstanceConfigurationRepository configurationRepository =
        mock(McpIntegrationInstanceConfigurationRepository.class);
    private final McpIntegrationInstanceConfigurationWorkflowRepository workflowRepository =
        mock(McpIntegrationInstanceConfigurationWorkflowRepository.class);
    private final McpIntegrationInstanceToolRepository toolRepository =
        mock(McpIntegrationInstanceToolRepository.class);
    private final McpIntegrationInstanceConfigurationOwnershipResolver configurationResolver =
        new McpIntegrationInstanceConfigurationOwnershipResolver(configurationRepository);
    private final McpIntegrationInstanceConfigurationWorkflowOwnershipResolver workflowResolver =
        new McpIntegrationInstanceConfigurationWorkflowOwnershipResolver(workflowRepository);
    private final McpIntegrationInstanceToolOwnershipResolver toolResolver =
        new McpIntegrationInstanceToolOwnershipResolver(toolRepository);

    @Test
    void testResourceTypesMatchTheGateTokens() {
        assertThat(configurationResolver.resourceType()).isEqualTo("McpIntegrationInstanceConfiguration");
        assertThat(workflowResolver.resourceType()).isEqualTo("McpIntegrationInstanceConfigurationWorkflow");
        assertThat(toolResolver.resourceType()).isEqualTo("McpIntegrationInstanceTool");
    }

    @Test
    void testResolvesAnEmbeddedRow() {
        when(configurationRepository.existsById(1L)).thenReturn(true);
        when(workflowRepository.existsById(2L)).thenReturn(true);
        when(toolRepository.existsById(3L)).thenReturn(true);

        assertThat(configurationResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(configurationResolver.resolveOwner(Long.valueOf(1L))).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(workflowResolver.resolveOwner(2L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(workflowResolver.resolveOwner(Long.valueOf(2L))).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(toolResolver.resolveOwner(3L)).isEqualTo(ResourceOwner.ofEmbedded());
        assertThat(toolResolver.resolveOwner(Long.valueOf(3L))).isEqualTo(ResourceOwner.ofEmbedded());
    }

    @Test
    void testAMissingRowIsUnknown() {
        assertThat(configurationResolver.resolveOwner(1L)).isEqualTo(ResourceOwner.unknown());
        assertThat(workflowResolver.resolveOwner(2L)).isEqualTo(ResourceOwner.unknown());
        assertThat(toolResolver.resolveOwner(3L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testANonNumericIdIsUnknown() {
        assertThat(configurationResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
        assertThat(workflowResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
        assertThat(toolResolver.resolveOwner("not-a-number")).isEqualTo(ResourceOwner.unknown());
    }
}
