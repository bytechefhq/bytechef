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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Registers the {@code mcpManagerChatClient} Spring bean used by the ai_hub BUILD agent.
 *
 * <p>
 * The mcp_manager subagent is a dedicated {@link ChatClient} pre-loaded with the MCP server lifecycle tools
 * ({@code listMcpServers}, {@code createMcpServer}, {@code updateMcpServer}, {@code createMcpProject},
 * {@code cloneMcpProject}) plus the workflow tool-mapping tools ({@code listMcpProjectWorkflows},
 * {@code updateMcpProjectWorkflowParameters}) and the {@code prompt_mcp_manager.txt} system prompt, which carries the
 * fromAi authoring playbook. Its isolated context means the parent ai_hub BUILD agent never sees the setup transcript —
 * it only receives the final status summary.
 * </p>
 *
 * <p>
 * The {@link ManagerSubAgentToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in the
 * ai_hub BUILD agent bean method (via {@link #createMcpManagerToolCallback}) so that it is registered only on that
 * agent.
 * </p>
 *
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class McpManagerConfiguration {

    static final String TOOL_DESCRIPTION = """
        Delegate MCP server management to a specialised mcp_manager subagent. It creates and configures
        MCP servers that expose selected workflows as MCP tools to external MCP clients (Claude, Cursor,
        etc.): create/enable/disable servers, attach a published project version's workflows, and complete
        each workflow's tool mapping — tool name, tool description, and fromAi(...) parameter expressions
        that define which inputs the calling agent supplies. Use for any request like "expose this
        workflow over MCP", "set up an MCP server", "make these workflows available to Claude", or "fix
        the tool schema of my MCP workflow". Pass the user's request verbatim in 'request', plus any ids
        (project, workflow, server) and decisions already resolved in this conversation. The subagent
        returns a status summary including the server URL-relevant ids; if it reports missing decisions
        (which project, which workflows, naming), relay them to the user with askUserQuestion and
        re-delegate with the answers.""";

    @Bean
    @ConditionalOnBean({
        McpProjectFacade.class, WorkspaceMcpServerFacade.class
    })
    ChatClient mcpManagerChatClient(
        ChatModel chatModel, McpProjectFacade mcpProjectFacade, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        WorkflowService workflowService, WorkspaceMcpServerFacade workspaceMcpServerFacade,
        @Value("classpath:prompt_mcp_manager.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(
                new ListMcpServersToolCallback(workspaceMcpServerFacade),
                new CreateMcpServerToolCallback(workspaceMcpServerFacade),
                new UpdateMcpServerToolCallback(workspaceMcpServerFacade),
                new CreateMcpProjectToolCallback(mcpProjectFacade),
                new CloneMcpProjectToolCallback(mcpProjectFacade),
                new ListMcpProjectWorkflowsToolCallback(
                    mcpProjectService, mcpProjectWorkflowService, projectDeploymentWorkflowService, workflowService),
                new UpdateMcpProjectWorkflowParametersToolCallback(mcpProjectWorkflowService))
            .build();
    }

    public static ManagerSubAgentToolCallback createMcpManagerToolCallback(ChatClient mcpManagerChatClient) {
        return new ManagerSubAgentToolCallback(ManagerAgentType.MCP_MANAGER, mcpManagerChatClient, TOOL_DESCRIPTION);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read mcp manager prompt resource: " + resource.getDescription(), exception);
        }
    }
}
