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
import com.bytechef.ai.copilot.tool.ClusterElementAgentToolCallback;
import com.bytechef.ai.copilot.tool.CodeEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.ConverterAgentToolCallback;
import com.bytechef.ai.copilot.tool.DataTableAgentToolCallback;
import com.bytechef.ai.copilot.tool.KnowledgeBaseAgentToolCallback;
import com.bytechef.ai.copilot.tool.SkillsAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowExecutionAgentToolCallback;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.configuration.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
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
class ToolCallbackContributorConfiguration {

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        @Qualifier("workflowEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> workflowEditorProvider,
        @Qualifier("codeEditorBuildSubAgentChatClient") ObjectProvider<ChatClient> codeEditorProvider,
        @Qualifier("clusterElementBuildSubAgentChatClient") ObjectProvider<ChatClient> clusterElementProvider,
        @Qualifier("skillsBuildSubAgentChatClient") ObjectProvider<ChatClient> skillsProvider,
        @Qualifier("workflowExecutionBuildSubAgentChatClient") ObjectProvider<ChatClient> workflowExecutionProvider,
        @Qualifier("converterBuildSubAgentChatClientSupplier") //
        ObjectProvider<Supplier<ChatClient>> converterSupplierProvider,
        @Qualifier("knowledgeBaseBuildSubAgentChatClient") ObjectProvider<ChatClient> knowledgeBaseProvider,
        @Qualifier("dataTableBuildSubAgentChatClient") ObjectProvider<ChatClient> dataTableProvider,
        @Qualifier("aiAgentBuildSubAgentChatClient") ObjectProvider<ChatClient> aiAgentProvider,
        @Qualifier("assetFileBuildSubAgentChatClient") ObjectProvider<ChatClient> assetFileProvider,
        WorkspaceService workspaceService) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            workflowEditorProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new WorkflowEditorAgentToolCallback(chatClient), workspaceService)));
            codeEditorProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new CodeEditorAgentToolCallback(chatClient), workspaceService)));
            clusterElementProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ClusterElementAgentToolCallback(chatClient), workspaceService)));
            skillsProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new SkillsAgentToolCallback(chatClient), workspaceService)));
            workflowExecutionProvider.ifAvailable(
                chatClient -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new WorkflowExecutionAgentToolCallback(chatClient), workspaceService)));
            converterSupplierProvider.ifAvailable(
                converterChatClientSupplier -> toolCallbacks.add(
                    new WorkspaceScopedSubAgentToolCallback(
                        new ConverterAgentToolCallback(converterChatClientSupplier), workspaceService)));
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
