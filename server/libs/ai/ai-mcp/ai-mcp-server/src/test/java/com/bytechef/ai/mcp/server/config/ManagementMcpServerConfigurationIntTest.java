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
import com.bytechef.automation.ai.tool.EnvironmentScopedToolCallback;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.WorkspaceTools;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.modelcontextprotocol.server.McpAsyncServer;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ManagementMcpServerConfigurationIntTest {

    private static final List<String> WORKSPACE_SCOPED_TOOL_NAMES = List.of(
        "listProjects", "createProject", "searchWorkflows");

    private final ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
        .withBean(ProjectTools.class, () -> mock(ProjectTools.class))
        .withBean(ProjectWorkflowLifecycleTools.class, () -> mock(ProjectWorkflowLifecycleTools.class))
        .withBean(WorkspaceTools.class, () -> mock(WorkspaceTools.class))
        .withUserConfiguration(ManagementMcpServerConfiguration.class, JacksonConfiguration.class)
        .withBean(ObjectMapper.class, () -> JsonMapper.builder()
            .build())
        .withBean(XmlMapper.class, () -> XmlMapper.builder()
            .build());

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
    void testToolCallbackProviderExposesTheValidatingUpdateWorkflowButNoPartialWrites() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(getToolNames(context.getBean(ManagementMcpServerConfiguration.class)))
                .contains("deleteWorkflow", "saveWorkflowTestConnection", "updateWorkflow", "createProjectWorkflow")
                .doesNotContain(
                    "getActionDefinition", "listComponents", "getTaskProperties",
                    "listTaskDispatchers", "updateScriptComponentCode", "updateClusterElementTask",
                    "updateWorkflowRootProperties");
        });
    }

    @Test
    void testToolCallbackProviderExposesOneToolPerProjectAndWorkflowRead() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(getToolNames(context.getBean(ManagementMcpServerConfiguration.class)))
                .containsExactlyInAnyOrder(
                    "listWorkspaces", "listProjects", "getProject", "createProject", "updateProject",
                    "deleteProject", "publishProject", "getWorkflow", "listWorkflows", "searchWorkflows",
                    "deleteWorkflow", "saveWorkflowTestConnection", "updateWorkflow", "createProjectWorkflow");
        });
    }

    @Test
    void testToolCallbackProviderDropsTheNearDuplicateAndWorkflowCreationTools() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(getToolNames(context.getBean(ManagementMcpServerConfiguration.class)))
                .doesNotContain("searchProjects", "getProjectStatus");
        });
    }

    @Test
    void testWorkflowScopedWritesAcceptAnEnvironment() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ManagementMcpServerConfiguration configuration = context.getBean(ManagementMcpServerConfiguration.class);

            assertThat(Arrays.stream(configuration.toolCallbackProvider()
                .getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .filter(definition -> List.of("updateWorkflow", "saveWorkflowTestConnection")
                    .contains(definition.name())))
                        .hasSize(2)
                        .allSatisfy(definition -> assertThat(definition.inputSchema()).contains("\"environment\""));
        });
    }

    @Test
    void testServesTheAuthoringLoopOnceTheBuildInstructionsAreContributed() {
        ToolCallback contributedToolCallback = mock(ToolCallback.class);

        when(contributedToolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("getWorkflowBuildInstructions")
                .description("d")
                .inputSchema("{\"type\":\"object\"}")
                .build());

        McpServerToolCallbackContributor contributor = () -> List.of(contributedToolCallback);

        applicationContextRunner
            .withBean(McpServerToolCallbackContributor.class, () -> contributor)
            .run(context -> {
                assertThat(context).hasNotFailed();

                List<String> toolNames = getToolNames(context.getBean(ManagementMcpServerConfiguration.class));

                assertThat(ManagementMcpInstructions.build(toolNames)).contains("To author a workflow yourself");
            });
    }

    @Test
    void testListToolsTakeAnOptionalQuery() {
        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            ManagementMcpServerConfiguration configuration = context.getBean(ManagementMcpServerConfiguration.class);

            assertThat(Arrays.stream(configuration.toolCallbackProvider()
                .getToolCallbacks())
                .map(ToolCallback::getToolDefinition)
                .filter(definition -> List.of("listProjects", "listWorkflows")
                    .contains(definition.name())))
                        .hasSize(2)
                        .allSatisfy(definition -> assertThat(definition.inputSchema()).contains("\"query\""));
        });
    }

    @Test
    void testBuildsTheContributedCallbacksOnlyAfterJsonUtilsIsInitialized() {
        ToolCallback probeToolCallback = mock(ToolCallback.class);

        when(probeToolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("probe")
                .description("d")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build());

        McpServerToolCallbackContributor contributor = () -> List.of(
            new EnvironmentScopedToolCallback(probeToolCallback));

        JsonUtils.setObjectMapper(null);

        try {
            applicationContextRunner
                .withBean(McpServerToolCallbackContributor.class, () -> contributor)
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(getToolNames(context.getBean(ManagementMcpServerConfiguration.class)))
                        .contains("probe");
                });
        } finally {
            JsonUtils.setObjectMapper(
                JsonMapper.builder()
                    .build());
        }
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

    private static List<String> getToolNames(ManagementMcpServerConfiguration configuration) {
        return Arrays.stream(configuration.toolCallbackProvider()
            .getToolCallbacks())
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
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
