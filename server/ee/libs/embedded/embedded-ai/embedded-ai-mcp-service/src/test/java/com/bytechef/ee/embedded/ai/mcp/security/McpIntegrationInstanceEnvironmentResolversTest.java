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

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationRepository;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceConfigurationWorkflowRepository;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationInstanceToolRepository;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class McpIntegrationInstanceEnvironmentResolversTest {

    private final McpServerRepository mcpServerRepository = mock(McpServerRepository.class);
    private final McpIntegrationInstanceConfigurationRepository configurationRepository =
        mock(McpIntegrationInstanceConfigurationRepository.class);
    private final McpIntegrationInstanceConfigurationWorkflowRepository workflowRepository =
        mock(McpIntegrationInstanceConfigurationWorkflowRepository.class);
    private final McpIntegrationInstanceToolRepository toolRepository =
        mock(McpIntegrationInstanceToolRepository.class);
    private final McpIntegrationInstanceConfigurationEnvironmentResolver configurationResolver =
        new McpIntegrationInstanceConfigurationEnvironmentResolver(configurationRepository, mcpServerRepository);
    private final McpIntegrationInstanceConfigurationWorkflowEnvironmentResolver workflowResolver =
        new McpIntegrationInstanceConfigurationWorkflowEnvironmentResolver(
            configurationRepository, workflowRepository, mcpServerRepository);
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService =
        mock(IntegrationInstanceConfigurationService.class);
    private final IntegrationInstanceService integrationInstanceService = mock(IntegrationInstanceService.class);
    private final McpIntegrationInstanceToolEnvironmentResolver toolResolver =
        new McpIntegrationInstanceToolEnvironmentResolver(
            integrationInstanceConfigurationService, integrationInstanceService, toolRepository);

    @Test
    void testResourceTypesMatchTheGateTokens() {
        assertThat(configurationResolver.resourceType()).isEqualTo("McpIntegrationInstanceConfiguration");
        assertThat(workflowResolver.resourceType()).isEqualTo("McpIntegrationInstanceConfigurationWorkflow");
        assertThat(toolResolver.resourceType()).isEqualTo("McpIntegrationInstanceTool");
    }

    @Test
    void testTheEnvironmentComesFromTheMcpServer() {
        McpServer mcpServer = new McpServer("server", PlatformType.EMBEDDED, Environment.STAGING);
        McpIntegrationInstanceConfiguration configuration = new McpIntegrationInstanceConfiguration(1L, 11L);
        McpIntegrationInstanceConfigurationWorkflow workflow = new McpIntegrationInstanceConfigurationWorkflow(1L, 5L);

        when(mcpServerRepository.findById(11L)).thenReturn(Optional.of(mcpServer));
        when(configurationRepository.findById(1L)).thenReturn(Optional.of(configuration));
        when(workflowRepository.findById(2L)).thenReturn(Optional.of(workflow));

        assertThat(configurationResolver.fetchEnvironment(1L)).contains(Environment.STAGING);
        assertThat(workflowResolver.fetchEnvironment(2L)).contains(Environment.STAGING);
    }

    @Test
    void testAMissingRowHasNoEnvironment() {
        assertThat(configurationResolver.fetchEnvironment(1L)).isEmpty();
        assertThat(workflowResolver.fetchEnvironment(2L)).isEmpty();
        assertThat(toolResolver.fetchEnvironment(3L)).isEmpty();
    }

    @Test
    void testANonNumericIdHasNoEnvironment() {
        assertThat(configurationResolver.fetchEnvironment("not-a-number")).isEmpty();
        assertThat(workflowResolver.fetchEnvironment("not-a-number")).isEmpty();
        assertThat(toolResolver.fetchEnvironment("not-a-number")).isEmpty();
    }

    @Test
    void testTheToolEnvironmentComesFromItsIntegrationInstanceNotTheServer() {
        McpIntegrationInstanceTool tool = new McpIntegrationInstanceTool(9L, 3L, true);
        IntegrationInstance integrationInstance = mock(IntegrationInstance.class);
        IntegrationInstanceConfiguration configuration = new IntegrationInstanceConfiguration();

        configuration.setEnvironment(Environment.PRODUCTION);

        when(integrationInstance.getIntegrationInstanceConfigurationId()).thenReturn(5L);
        when(toolRepository.findById(3L)).thenReturn(Optional.of(tool));
        when(integrationInstanceService.getIntegrationInstances(List.of(9L))).thenReturn(List.of(integrationInstance));
        when(integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(List.of(5L)))
            .thenReturn(List.of(configuration));

        assertThat(toolResolver.fetchEnvironment(3L)).contains(Environment.PRODUCTION);
    }

    @Test
    void testAToolWithAMissingInstanceHasNoEnvironment() {
        McpIntegrationInstanceTool tool = new McpIntegrationInstanceTool(9L, 3L, true);

        when(toolRepository.findById(3L)).thenReturn(Optional.of(tool));
        when(integrationInstanceService.getIntegrationInstances(List.of(9L))).thenReturn(List.of());

        assertThat(toolResolver.fetchEnvironment(3L)).isEmpty();
    }
}
