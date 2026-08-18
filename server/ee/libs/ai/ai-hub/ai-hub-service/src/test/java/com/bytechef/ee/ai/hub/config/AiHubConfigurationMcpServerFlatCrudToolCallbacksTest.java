/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the six MCP server CRUD tool names {@link AiHubConfiguration#mcpServerFlatCrudToolCallbacks} restores flat on
 * the hub surfaces (ticket 732, Task 3) and confirms the seventh factory-list member,
 * {@code updateMcpProjectWorkflowParameters}, stays exclusive to the {@code configureMcpServer} intelligent tool.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationMcpServerFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of("listMcpServers", "listMcpProjectWorkflows");

    private static final Set<String> EXPECTED_WRITE_NAMES = Set.of(
        "listMcpServers", "listMcpProjectWorkflows", "createMcpServer", "updateMcpServer", "createMcpProject",
        "cloneMcpProject");

    @Test
    void testReadModeReturnsExactlyTheTwoReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.mcpServerFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsExactlyTheSixNamesExcludingTheMappingTool() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.mcpServerFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_WRITE_NAMES)
            .doesNotContain("updateMcpProjectWorkflowParameters");
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.mcpServerFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.mcpServerFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpServerToolCallbacksFactory> present() {
        McpServerToolCallbacksFactory factory = new McpServerToolCallbacksFactory(
            mock(McpProjectFacade.class), mock(McpProjectService.class), mock(McpProjectWorkflowService.class),
            mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class),
            mock(WorkspaceMcpServerFacade.class));

        ObjectProvider<McpServerToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<McpServerToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
