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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.bytechef.ai.copilot.tool.ConfigureMcpServerToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
final class McpServerSubAgentConfigurationTest {

    private final McpServerSubAgentConfiguration configuration = newConfiguration();

    @Test
    void testMcpServerBuildSubAgentChatClientIsBuilt() {
        assertThatNoException().isThrownBy(
            () -> configuration.mcpServerBuildSubAgentChatClient(
                mock(ChatModel.class), mock(McpProjectService.class), mock(McpProjectWorkflowService.class),
                mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class)));
    }

    @Test
    void testFactoryReturnsFixedClientWhenNoChatModelSupplied() {
        ChatClient fixedChatClient = mock(ChatClient.class);

        IntelligentToolChatClientFactory factory = configuration.mcpServerBuildSubAgentChatClientFactory(
            fixedChatClient, mock(McpProjectService.class), mock(McpProjectWorkflowService.class),
            mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class));

        assertThat(factory.get(null)).isSameAs(fixedChatClient);
    }

    @Test
    void testFactoryRebuildsClientWhenChatModelSupplied() {
        ChatClient fixedChatClient = mock(ChatClient.class);

        IntelligentToolChatClientFactory factory = configuration.mcpServerBuildSubAgentChatClientFactory(
            fixedChatClient, mock(McpProjectService.class), mock(McpProjectWorkflowService.class),
            mock(ProjectDeploymentWorkflowService.class), mock(WorkflowService.class));

        ChatClient rebuilt = factory.get(mock(ChatModel.class));

        assertThat(rebuilt).isNotNull()
            .isNotSameAs(fixedChatClient);
    }

    @Test
    void testConfigureMcpServerToolCallbackDescriptionMentionsFromAi() {
        ToolCallback toolCallback = new ConfigureMcpServerToolCallback(chatModel -> mock(ChatClient.class), null);

        String description = toolCallback.getToolDefinition()
            .description();

        assertThat(description).contains("fromAi");
        assertThat(description.toLowerCase()).contains("mcp server");
    }

    private static McpServerSubAgentConfiguration newConfiguration() {
        McpServerSubAgentConfiguration configuration = new McpServerSubAgentConfiguration();

        setResourceField(configuration, "promptResource", "prompt_mcp_agent.txt");

        return configuration;
    }

    private static void setResourceField(
        McpServerSubAgentConfiguration configuration, String fieldName, String classpathResource) {

        try {
            Field field = McpServerSubAgentConfiguration.class.getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(configuration, new ClassPathResource(classpathResource));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
