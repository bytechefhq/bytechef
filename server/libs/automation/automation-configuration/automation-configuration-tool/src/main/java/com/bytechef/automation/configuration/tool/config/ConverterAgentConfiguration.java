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

package com.bytechef.automation.configuration.tool.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.agent.OverrideChatClientResolver;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.automation.configuration.tool.AutomationConfigurationAgentType;
import com.bytechef.automation.configuration.tool.ProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ScriptTools;
import com.bytechef.automation.configuration.tool.agent.ConverterSpringAIAgent;
import com.bytechef.automation.data.table.tool.DataTableTools;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @author Marko Kriskovic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class ConverterAgentConfiguration {

    private static final String CONVERTER_SUBAGENT_ADDENDUM = """

        ## Target workflow

        You are a one-shot subagent. Your caller gives you EITHER a `workflowId` (the workflow already exists) OR a
        `projectId` (the workflow does not exist yet).

        Build and validate the COMPLETE converted definition first, then persist it with ONE call:

        - Given a `workflowId` (a leading `Target workflowId:` line counts) - `getWorkflow(workflowId)` for its
          current `version`, then `updateWorkflow(workflowId, workflow, version)`.
        - Given only a `projectId` - ONE `createProjectWorkflow(projectId, workflow)` call carrying the whole
          definition. Never create an empty or placeholder workflow and fill it in afterwards.

        If you are given both, the `workflowId` wins; never create a second workflow. You cannot create a PROJECT.
        Given neither a `workflowId` nor a `projectId`, say so plainly and tell the caller to create or choose the
        project first, then re-invoke you with its `projectId`.
        """;
    private final Resource promptConverterBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();
    private final String converterBuildSystemPrompt;
    private final @Nullable DataTableTools dataTableTools;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public ConverterAgentConfiguration(
        @Value("classpath:prompt_converter_build.txt") Resource promptConverterBuildResource,
        WorkflowValidatorTools workflowValidatorTools,
        WorkflowInstructionTools workflowInstructionTools,
        ObjectProvider<DataTableTools> dataTableToolsProvider) {

        this.dataTableTools = dataTableToolsProvider.getIfAvailable();
        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptConverterBuildResource = promptConverterBuildResource;
        this.converterBuildSystemPrompt = getSystemPrompt(promptConverterBuildResource);
    }

    private static List<ToolCallback> getCurrentWebSearchToolCallbacks(
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        Environment environment = EnvironmentContext.fetchCurrentEnvironment();

        if (environment == null) {
            return List.of();
        }

        return webSearchToolCallbackProvider.getToolCallbacks(environment.ordinal());
    }

    private List<ToolCallback> wrapTools(SecurityContextRehydrator securityContextRehydrator, List<Object> tools) {
        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (Object tool : tools) {
            if (tool instanceof ToolCallback toolCallback) {
                toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
            } else {
                for (ToolCallback toolCallback : ToolCallbacks.from(tool)) {
                    toolCallbacks.add(RehydrateContextToolCallback.wrap(toolCallback, securityContextRehydrator));
                }
            }
        }

        return toolCallbacks;
    }

    @Bean
    ConverterSpringAIAgent converterBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return ConverterSpringAIAgent.builder()
            .agentId(AutomationConfigurationAgentType.CONVERTER_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptConverterBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    public ChatClient converterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools) {

        return buildConverterBuildSubAgentChatClient(
            chatModel, projectWorkflowTools, taskTools, scriptTools, List.of());
    }

    @Bean
    public IntelligentToolChatClientFactory converterBuildSubAgentChatClientFactory(
        @Qualifier("converterBuildSubAgentChatClient") ChatClient converterBuildSubAgentChatClient,
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools, ScriptTools scriptTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return converterBuildSubAgentChatClient;
            }

            return buildConverterBuildSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, projectWorkflowTools, taskTools,
                scriptTools, webSearchToolCallbacks);
        };
    }

    private ChatClient buildConverterBuildSubAgentChatClient(
        ChatModel chatModel, ProjectWorkflowTools projectWorkflowTools, TaskTools taskTools,
        ScriptTools scriptTools, List<ToolCallback> webSearchToolCallbacks) {

        return ChatClient.builder(chatModel)
            .defaultSystem(converterSubAgentSystemPrompt())
            .defaultTools(
                withDataTableTools(
                    projectWorkflowTools, taskTools, scriptTools, workflowValidatorTools, workflowInstructionTools))
            .defaultToolCallbacks(webSearchToolCallbacks)
            .build();
    }

    private Object[] withDataTableTools(Object... tools) {
        List<Object> allTools = new ArrayList<>(List.of(tools));

        if (dataTableTools != null) {
            allTools.add(dataTableTools);
        }

        return allTools.toArray();
    }

    String converterSubAgentSystemPrompt() {
        return converterBuildSystemPrompt + CONVERTER_SUBAGENT_ADDENDUM;
    }

    private String getSystemPrompt(Resource systemPromptResource) {
        try {
            InputStream inputStream = systemPromptResource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read system prompt resource: " + systemPromptResource.getDescription(), exception);
        }
    }
}
