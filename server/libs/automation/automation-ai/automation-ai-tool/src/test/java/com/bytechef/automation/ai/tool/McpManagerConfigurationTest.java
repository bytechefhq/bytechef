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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 *
 * @author Ivica Cardic
 */
class McpManagerConfigurationTest {

    private final McpManagerConfiguration mcpManagerConfiguration = new McpManagerConfiguration();

    @Test
    void testMcpManagerChatClientIsBuilt() {
        Resource promptResource = new ByteArrayResource(
            "You are the mcp_manager subagent.".getBytes(StandardCharsets.UTF_8), "test prompt_mcp_manager.txt");

        assertThatNoException().isThrownBy(
            () -> mcpManagerConfiguration.mcpManagerChatClient(
                mock(ChatModel.class), mock(McpProjectFacade.class), mock(McpProjectService.class),
                mock(McpProjectWorkflowService.class), mock(ProjectDeploymentWorkflowService.class),
                mock(WorkflowService.class), mock(WorkspaceMcpServerFacade.class), promptResource));
    }

    @Test
    void testMcpManagerToolCallbackIsNamedCorrectly() {
        ToolCallback toolCallback = McpManagerConfiguration.createMcpManagerToolCallback(mock(ChatClient.class));

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("mcp_manager");
    }

    @Test
    void testMcpManagerToolCallbackDescriptionMentionsFromAi() {
        ToolCallback toolCallback = McpManagerConfiguration.createMcpManagerToolCallback(mock(ChatClient.class));

        String description = toolCallback.getToolDefinition()
            .description();

        assertThat(description).contains("fromAi");
        assertThat(description.toLowerCase()).contains("mcp server");
    }
}
