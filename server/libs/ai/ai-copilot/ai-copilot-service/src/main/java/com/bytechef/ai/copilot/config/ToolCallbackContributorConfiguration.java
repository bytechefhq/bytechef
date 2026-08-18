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

import com.bytechef.ai.copilot.tool.AiAgentAgentToolCallback;
import com.bytechef.ai.copilot.tool.AssetFileAgentToolCallback;
import com.bytechef.ai.copilot.tool.DataTableAgentToolCallback;
import com.bytechef.ai.copilot.tool.KnowledgeBaseAgentToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ToolCallbackContributorConfiguration {

    /**
     * Names of the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition}s this contributor owns on the
     * management MCP surface, contributed by {@link CopilotIntelligentToolContributor}. Filtered with
     * {@link IntelligentToolCatalog#getByNames} over its own name partition, never the whole catalog, because later EE
     * intelligent-tool contributors (custom_component, code_workflow, integration_workflow) register their own
     * management-MCP contributor configs against the same catalog — {@code getAll} here would double-register their
     * delegates.
     *
     * <p>
     * Public so {@code IntelligentToolSurfaceParityTest} (ai-hub-service) can assert this set is disjoint from, and
     * unions with, the other management-MCP surfaces' name sets to equal the full catalog.
     * </p>
     */
    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript", "authorSkill",
        "debugWorkflowExecution");

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        @Qualifier("knowledgeBaseBuildSubAgentChatClient") ObjectProvider<ChatClient> knowledgeBaseProvider,
        @Qualifier("dataTableBuildSubAgentChatClient") ObjectProvider<ChatClient> dataTableProvider,
        @Qualifier("aiAgentBuildSubAgentChatClient") ObjectProvider<ChatClient> aiAgentProvider,
        @Qualifier("assetFileBuildSubAgentChatClient") ObjectProvider<ChatClient> assetFileProvider,
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            toolCallbacks.addAll(
                intelligentToolCatalog.getByNames(
                    INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                    (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                        toolCallback, workspaceService)));

            knowledgeBaseProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new KnowledgeBaseAgentToolCallback(chatClient), workspaceService)));
            dataTableProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new DataTableAgentToolCallback(chatClient), workspaceService)));
            aiAgentProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new AiAgentAgentToolCallback(chatClient), workspaceService)));
            // MCP has no ASK/BUILD concept and always injects the write-capable chat client, so the tool
            // description must always advertise the write tool set.
            assetFileProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new AssetFileAgentToolCallback(chatClient, true), workspaceService)));

            return toolCallbacks;
        };
    }
}
