/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.annotation.Bean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ManagementMcpServerToolCallbackProviderTest {

    @Test
    void includesContributedCallbacksAlongsideDirectTools() {
        ToolCallback contributed = mock(ToolCallback.class);

        when(contributed.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("workflow_editor_agent")
                .description("d")
                .inputSchema("{\"type\":\"object\"}")
                .build());

        McpServerToolCallbackContributor contributor = () -> List.of(contributed);

        ManagementMcpServerConfiguration configuration = new ManagementMcpServerConfiguration(
            mock(ComponentTools.class), mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
            mock(TaskTools.class), mock(TaskDispatcherTools.class), mock(ScriptTools.class),
            mock(ClusterElementTools.class), List.of(contributor));

        ToolCallbackProvider provider = configuration.toolCallbackProvider();

        List<String> names = Arrays.stream(provider.getToolCallbacks())
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();

        assertThat(names).contains("workflow_editor_agent");
    }

    @Test
    void worksWithNoContributors() {
        ManagementMcpServerConfiguration configuration = new ManagementMcpServerConfiguration(
            mock(ComponentTools.class), mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
            mock(TaskTools.class), mock(TaskDispatcherTools.class), mock(ScriptTools.class),
            mock(ClusterElementTools.class), List.of());

        ToolCallbackProvider provider = configuration.toolCallbackProvider();

        assertThat(provider.getToolCallbacks()).isNotNull();
    }

    @Test
    void toolCallbackProviderIsNotPublishedAsBean() throws NoSuchMethodException {
        Method toolCallbackProviderMethod = ManagementMcpServerConfiguration.class.getDeclaredMethod(
            "toolCallbackProvider");

        assertThat(toolCallbackProviderMethod.isAnnotationPresent(Bean.class))
            .as("toolCallbackProvider() must not be a @Bean or the ChatModel tool-calling cycle returns")
            .isFalse();

        Method mcpAsyncServerMethod = ManagementMcpServerConfiguration.class.getDeclaredMethod("mcpAsyncServer");

        assertThat(mcpAsyncServerMethod.isAnnotationPresent(Bean.class))
            .as("mcpAsyncServer() must remain a @Bean so the MCP endpoint is wired")
            .isTrue();
    }
}
