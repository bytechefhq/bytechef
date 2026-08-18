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

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the {@code mcpServerBuildSubAgentChatClient} / {@code mcpServerBuildSubAgentChatClientFactory} Spring beans
 * that back the {@code configureMcpServer} intelligent tool — the MCP-server tool-mapping subagent consumed by the
 * ai_hub BUILD agent, the management MCP server, and the MCP Servers Copilot panel (see
 * {@link McpServerIntelligentToolContributor}, which registers the {@code configureMcpServer}
 * {@code IntelligentToolDefinition} against this factory).
 *
 * <p>
 * The subagent carries exactly two CRUD tools — {@link ListMcpProjectWorkflowsToolCallback} and
 * {@link UpdateMcpProjectWorkflowParametersToolCallback} — plus the {@code prompt_mcp_agent.txt} system prompt, which
 * carries the fromAi authoring playbook narrowed to tool-mapping synthesis. It does not create, attach to, or enable
 * servers; those remain flat CRUD tools on whichever surface calls this subagent.
 * </p>
 *
 * <p>
 * Gated so the beans exist when either the Copilot panel or the AI Hub surface is enabled, since both consume this
 * subagent — mirrors {@code DataTableAgentConfiguration}.
 * </p>
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class McpServerSubAgentConfiguration {

    @Value("classpath:prompt_mcp_agent.txt")
    private Resource promptResource;

    @Bean
    @ConditionalOnBean({
        McpProjectService.class, McpProjectWorkflowService.class
    })
    ChatClient mcpServerBuildSubAgentChatClient(
        ChatModel chatModel, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        return buildMcpServerBuildSubAgentChatClient(
            chatModel, mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService,
            workflowService);
    }

    @Bean
    @ConditionalOnBean({
        McpProjectService.class, McpProjectWorkflowService.class
    })
    IntelligentToolChatClientFactory mcpServerBuildSubAgentChatClientFactory(
        @Qualifier("mcpServerBuildSubAgentChatClient") ChatClient mcpServerBuildSubAgentChatClient,
        McpProjectService mcpProjectService, McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        return candidateChatModel -> candidateChatModel == null
            ? mcpServerBuildSubAgentChatClient
            : buildMcpServerBuildSubAgentChatClient(
                candidateChatModel, mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService,
                workflowService);
    }

    private ChatClient buildMcpServerBuildSubAgentChatClient(
        ChatModel chatModel, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, WorkflowService workflowService) {

        return ChatClient.builder(chatModel)
            .defaultSystem(readPrompt(promptResource))
            .defaultTools(
                new ListMcpProjectWorkflowsToolCallback(
                    mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService, workflowService),
                new UpdateMcpProjectWorkflowParametersToolCallback(mcpProjectWorkflowService))
            .build();
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read mcp server prompt resource: " + resource.getDescription(), exception);
        }
    }
}
