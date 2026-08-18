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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.SliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.ConfigureMcpServerToolCallback;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
final class McpServerAgentConfigurationTest {

    private final McpServerAgentConfiguration configuration = newConfiguration();

    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

    private final McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = new McpServerToolCallbacksFactory(
        mock(McpProjectFacade.class), mock(McpProjectService.class), mock(McpProjectWorkflowService.class),
        mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class),
        mock(WorkspaceMcpServerFacade.class));

    @Test
    void testAskAgentUsesReadToolsAndBuildAgentUsesWriteTools() throws AGUIException {
        SliceSpringAIAgent askAgent = configuration.mcpServerAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), mcpServerToolCallbacksFactory,
            securityContextRehydrator, emptyProvider());

        SliceSpringAIAgent buildAgent = configuration.mcpServerBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), mcpServerToolCallbacksFactory,
            securityContextRehydrator, emptyCatalog(), emptyProvider());

        assertThat(askAgent.getAgentId()).isEqualTo("mcp_server_ask");
        assertThat(buildAgent.getAgentId()).isEqualTo("mcp_server_build");

        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(
                securityContextRehydrator, mcpServerToolCallbacksFactory, emptyCatalog()));

        assertThat(buildToolNames).containsExactlyInAnyOrder(
            "listMcpServers", "listMcpProjectWorkflows", "createMcpServer", "updateMcpServer", "createMcpProject",
            "cloneMcpProject", "updateMcpProjectWorkflowParameters");
    }

    @Test
    void testBuildToolCallbacksIncludesConfigureMcpServerFromTheCatalog() {
        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(
                securityContextRehydrator, mcpServerToolCallbacksFactory, catalogWithConfigureMcpServer()));

        assertThat(buildToolNames).contains("configureMcpServer");
    }

    @Test
    void testAskAgentToolsAreReadOnly() {
        List<String> askToolNames = toolNames(
            configuration.askToolCallbacks(securityContextRehydrator, mcpServerToolCallbacksFactory));

        assertThat(askToolNames).containsExactly("listMcpServers", "listMcpProjectWorkflows");
    }

    private static McpServerAgentConfiguration newConfiguration() {
        McpServerAgentConfiguration configuration = new McpServerAgentConfiguration();

        setResourceField(configuration, "promptMcpServerAskResource", "prompt_mcp_server_ask.txt");
        setResourceField(configuration, "promptMcpServerBuildResource", "prompt_mcp_server_build.txt");

        return configuration;
    }

    private static void setResourceField(
        McpServerAgentConfiguration configuration, String fieldName, String classpathResource) {

        try {
            Field field = McpServerAgentConfiguration.class.getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(configuration, new ClassPathResource(classpathResource));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    private static IntelligentToolCatalog emptyCatalog() {
        return catalogOf();
    }

    private static IntelligentToolCatalog catalogWithConfigureMcpServer() {
        return catalogOf(
            new SimpleIntelligentToolDefinition(
                "configureMcpServer", CopilotAgentType.CONFIGURE_MCP_SERVER.key(),
                Set.of(IntelligentToolScope.MCP_SERVER),
                variant -> chatModel -> mock(ChatClient.class),
                chatClientFactory -> new ConfigureMcpServerToolCallback(chatClientFactory, null)));
    }

    private static IntelligentToolCatalog catalogOf(IntelligentToolDefinition... definitions) {
        IntelligentToolContributor contributor = () -> List.of(definitions);

        return new IntelligentToolCatalog(fixedContributorProvider(contributor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedContributorProvider(
        IntelligentToolContributor contributor) {

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return objectProvider;
    }
}
