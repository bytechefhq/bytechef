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

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.SkillsTools;
import com.bytechef.automation.ai.tool.WorkspaceScopeResolver;
import com.bytechef.automation.ai.tool.WorkspaceScopedFlatToolCallback;
import com.bytechef.automation.ai.tool.WorkspaceScopedSubAgentToolCallback;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
public class ToolCallbackContributorConfiguration {

    public static final Set<String> INTELLIGENT_TOOL_NAMES = Set.of(
        "buildWorkflow", "importWorkflow", "configureClusterElement", "writeScript", "authorSkill",
        "debugWorkflowExecution", "configureMcpServer");

    private static final String MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME = "updateMcpProjectWorkflowParameters";

    private static final Set<String> WORKSPACE_SCOPED_MCP_TOOL_NAMES = Set.of(
        "listMcpServers", "createMcpServer", "listMcpProjectWorkflows");

    private static final Set<String> WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES = Set.of("listProjectDeployments");

    @Bean
    McpServerToolCallbackContributor copilotAgentToolCallbackContributor(
        ObjectProvider<SkillsTools> skillsToolsProvider,
        IntelligentToolCatalog intelligentToolCatalog, WorkspaceScopeResolver workspaceScopeResolver) {

        return () -> {
            List<ToolCallback> toolCallbacks = new ArrayList<>();

            skillsToolsProvider.ifAvailable(
                skillsTools -> toolCallbacks.addAll(List.of(ToolCallbacks.from(skillsTools))));

            toolCallbacks.addAll(
                intelligentToolCatalog.getByNames(
                    INTELLIGENT_TOOL_NAMES, IntelligentToolVariant.BUILD, (chatClient, definition) -> chatClient,
                    (toolCallback, definition) -> new WorkspaceScopedSubAgentToolCallback(
                        toolCallback, workspaceScopeResolver)));

            return toolCallbacks;
        };
    }

    @Bean
    McpServerToolCallbackContributor mcpServerCrudMcpContributor(
        ObjectProvider<McpServerToolCallbacksFactory> mcpServerToolCallbacksFactoryProvider,
        WorkspaceScopeResolver workspaceScopeResolver) {

        return () -> {
            McpServerToolCallbacksFactory mcpServerToolCallbacksFactory = mcpServerToolCallbacksFactoryProvider
                .getIfAvailable();

            if (mcpServerToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : mcpServerToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                if (MCP_PROJECT_WORKFLOW_PARAMETERS_TOOL_NAME.equals(name)) {
                    continue;
                }

                toolCallbacks.add(
                    WORKSPACE_SCOPED_MCP_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceScopeResolver)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }

    @Bean
    McpServerToolCallbackContributor deploymentFlatCrudMcpContributor(
        ObjectProvider<DeploymentToolCallbacksFactory> deploymentToolCallbacksFactoryProvider,
        WorkspaceScopeResolver workspaceScopeResolver) {

        return () -> {
            DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = deploymentToolCallbacksFactoryProvider
                .getIfAvailable();

            if (deploymentToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : deploymentToolCallbacksFactory.writeToolCallbacks()) {
                String name = toolCallback.getToolDefinition()
                    .name();

                toolCallbacks.add(
                    WORKSPACE_SCOPED_DEPLOYMENT_TOOL_NAMES.contains(name)
                        ? new WorkspaceScopedFlatToolCallback(toolCallback, workspaceScopeResolver)
                        : toolCallback);
            }

            return toolCallbacks;
        };
    }

    @Bean
    McpServerToolCallbackContributor knowledgeBaseFlatCrudMcpContributor(
        ObjectProvider<KnowledgeBaseToolCallbacksFactory> knowledgeBaseToolCallbacksFactoryProvider,
        WorkspaceScopeResolver workspaceScopeResolver) {

        return () -> {
            KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory =
                knowledgeBaseToolCallbacksFactoryProvider
                    .getIfAvailable();

            if (knowledgeBaseToolCallbacksFactory == null) {
                return List.of();
            }

            List<ToolCallback> toolCallbacks = new ArrayList<>();

            for (ToolCallback toolCallback : knowledgeBaseToolCallbacksFactory.writeToolCallbacks()) {
                toolCallbacks.add(new WorkspaceScopedFlatToolCallback(toolCallback, workspaceScopeResolver));
            }

            return toolCallbacks;
        };
    }
}
