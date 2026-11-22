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

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceWorkflowRepository;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceWorkflowEnvironmentResolverTest {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final McpIntegrationInstanceWorkflowRepository mcpIntegrationInstanceWorkflowRepository =
        mock(McpIntegrationInstanceWorkflowRepository.class);
    private final McpIntegrationInstanceWorkflowEnvironmentResolver mcpIntegrationInstanceWorkflowEnvironmentResolver =
        new McpIntegrationInstanceWorkflowEnvironmentResolver(
            integrationInstanceConfigurationService, integrationInstanceService,
            mcpIntegrationInstanceWorkflowRepository);

    @Test
    void testResourceTypeMatchesTheGateToken() {
        assertThat(mcpIntegrationInstanceWorkflowEnvironmentResolver.resourceType())
            .isEqualTo("McpIntegrationInstanceWorkflow");
    }

    @Test
    void testTheEnvironmentComesFromItsIntegrationInstance() {
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
            new McpIntegrationInstanceWorkflow(9L, 6L, true);
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);
        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setEnvironment(Environment.PRODUCTION);

        when(integrationInstance.getIntegrationInstanceConfigurationId()).thenReturn(5L);
        when(mcpIntegrationInstanceWorkflowRepository.findById(4L))
            .thenReturn(Optional.of(mcpIntegrationInstanceWorkflow));
        when(integrationInstanceService.getIntegrationInstances(List.of(9L))).thenReturn(List.of(integrationInstance));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(5L)))
            .thenReturn(List.of(integrationInstanceConfiguration));

        assertThat(mcpIntegrationInstanceWorkflowEnvironmentResolver.fetchEnvironment(4L))
            .contains(Environment.PRODUCTION);
    }

    @Test
    void testAWorkflowWithAMissingInstanceHasNoEnvironment() {
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
            new McpIntegrationInstanceWorkflow(9L, 6L, true);

        when(mcpIntegrationInstanceWorkflowRepository.findById(4L))
            .thenReturn(Optional.of(mcpIntegrationInstanceWorkflow));
        when(integrationInstanceService.getIntegrationInstances(List.of(9L))).thenReturn(List.of());

        assertThat(mcpIntegrationInstanceWorkflowEnvironmentResolver.fetchEnvironment(4L)).isEmpty();
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        assertThat(mcpIntegrationInstanceWorkflowEnvironmentResolver.fetchEnvironment(4L)).isEmpty();
    }

    @Test
    void testANonNumericIdHasNoEnvironment() {
        assertThat(mcpIntegrationInstanceWorkflowEnvironmentResolver.fetchEnvironment("not-a-number")).isEmpty();
    }
}
