/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.ai.mcp.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.ClusterElementTools;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.ScriptTools;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import com.bytechef.platform.ai.tool.ComponentTools;
import com.bytechef.platform.ai.tool.TaskDispatcherTools;
import com.bytechef.platform.ai.tool.TaskTools;
import io.modelcontextprotocol.server.McpAsyncServer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Ivica Cardic
 */
class ManagementMcpServerConfigurationIntTest {

    private static final List<String> WORKSPACE_SCOPED_TOOL_NAMES = List.of(
        "listProjects", "searchProjects", "createProject", "searchWorkflows");

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withBean(ComponentTools.class, () -> mock(ComponentTools.class))
        .withBean(ProjectTools.class, () -> mock(ProjectTools.class))
        .withBean(ProjectWorkflowLifecycleTools.class, () -> mock(ProjectWorkflowLifecycleTools.class))
        .withBean(TaskTools.class, () -> mock(TaskTools.class))
        .withBean(TaskDispatcherTools.class, () -> mock(TaskDispatcherTools.class))
        .withBean(ScriptTools.class, () -> mock(ScriptTools.class))
        .withBean(ClusterElementTools.class, () -> mock(ClusterElementTools.class))
        .withBean(WorkspaceTools.class, () -> mock(WorkspaceTools.class))
        .withUserConfiguration(ManagementMcpServerConfiguration.class);

    @Test
    void testToolCallbackProviderIsNotRegisteredAsBean() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ToolCallbackProvider.class);
        });
    }

    @Test
    void testMcpAsyncServersAreRegisteredForTenantAnchoredAndCanonicalEndpoints() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(McpAsyncServer.class))
                .containsOnlyKeys("mcpAsyncServer", "canonicalMcpAsyncServer");
            assertThat(context.getBeansOfType(WebMvcStreamableServerTransportProvider.class))
                .containsOnlyKeys(
                    "webMvcStreamableHttpServerTransportProvider",
                    "canonicalWebMvcStreamableHttpServerTransportProvider");
        });
    }

    @Test
    void testIncludesContributedCallbacksAlongsideDirectTools() {
        ToolCallback contributedToolCallback = mock(ToolCallback.class);

        when(contributedToolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("buildWorkflow")
                .description("d")
                .inputSchema("{\"type\":\"object\"}")
                .build());

        McpServerToolCallbackContributor contributor = () -> List.of(contributedToolCallback);

        applicationContextRunner
            .withBean(McpServerToolCallbackContributor.class, () -> contributor)
            .run(context -> {
                assertThat(context).hasNotFailed();

                ManagementMcpServerConfiguration configuration =
                    context.getBean(ManagementMcpServerConfiguration.class);

                ToolCallbackProvider toolCallbackProvider = configuration.toolCallbackProvider();

                List<String> toolNames = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                    .map(toolCallback -> toolCallback.getToolDefinition()
                        .name())
                    .toList();

                assertThat(toolNames).contains("buildWorkflow");
            });
    }

    @Test
    void testToolCallbackProviderExposesLifecycleToolsWithoutUpdateWorkflow() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ManagementMcpServerConfiguration configuration = context.getBean(ManagementMcpServerConfiguration.class);

            List<String> toolNames = Arrays.stream(configuration.toolCallbackProvider()
                .getToolCallbacks())
                .map(toolCallback -> toolCallback.getToolDefinition()
                    .name())
                .toList();

            assertThat(toolNames)
                .contains("createProjectWorkflow", "deleteWorkflow", "saveWorkflowTestConnection")
                .doesNotContain("updateWorkflow");
        });
    }

    @Test
    void testCreateProjectWorkflowTakesNameNotDefinition() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ManagementMcpServerConfiguration configuration = context.getBean(ManagementMcpServerConfiguration.class);

            ToolDefinition toolDefinition = Arrays.stream(configuration.toolCallbackProvider()
                .getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .filter(definition -> "createProjectWorkflow".equals(definition.name()))
                .findFirst()
                .orElseThrow();

            assertThat(toolDefinition.inputSchema()).contains("\"name\"");
            assertThat(toolDefinition.inputSchema()).doesNotContain("definition");
        });
    }

    @Test
    void testWorksWithNoContributors() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(McpServerToolCallbackContributor.class);

            ManagementMcpServerConfiguration configuration = context.getBean(ManagementMcpServerConfiguration.class);

            ToolCallbackProvider toolCallbackProvider = configuration.toolCallbackProvider();

            assertThat(toolCallbackProvider.getToolCallbacks()).isNotNull();
        });
    }

    @Nested
    class WorkspaceScopedToolsTest {

        @Test
        void testListWorkspacesIsExposed() {
            applicationContextRunner.run(context -> {
                assertThat(context).hasNotFailed();

                ManagementMcpServerConfiguration configuration =
                    context.getBean(ManagementMcpServerConfiguration.class);

                ToolCallbackProvider toolCallbackProvider = configuration.toolCallbackProvider();

                assertThat(toolCallbackProvider.getToolCallbacks())
                    .extracting(toolCallback -> toolCallback.getToolDefinition()
                        .name())
                    .contains("listWorkspaces");
            });
        }

        @Test
        void testToolsThatWorkWithinAWorkspaceAcceptWorkspaceId() {
            applicationContextRunner.run(context -> {
                assertThat(context).hasNotFailed();

                ManagementMcpServerConfiguration configuration =
                    context.getBean(ManagementMcpServerConfiguration.class);

                ToolCallbackProvider toolCallbackProvider = configuration.toolCallbackProvider();

                List<ToolDefinition> toolDefinitions = Arrays.stream(toolCallbackProvider.getToolCallbacks())
                    .map(ToolCallback::getToolDefinition)
                    .filter(toolDefinition -> WORKSPACE_SCOPED_TOOL_NAMES.contains(toolDefinition.name()))
                    .toList();

                assertThat(toolDefinitions)
                    .extracting(ToolDefinition::name)
                    .containsExactlyInAnyOrderElementsOf(WORKSPACE_SCOPED_TOOL_NAMES);
                assertThat(toolDefinitions)
                    .allSatisfy(toolDefinition -> assertThat(toolDefinition.inputSchema()).contains("workspaceId"));
            });
        }
    }
}
