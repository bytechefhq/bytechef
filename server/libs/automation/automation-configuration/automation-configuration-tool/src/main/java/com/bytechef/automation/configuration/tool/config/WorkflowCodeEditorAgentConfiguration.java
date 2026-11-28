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
import com.bytechef.automation.configuration.tool.AutomationConfigurationAgentType;
import com.bytechef.automation.configuration.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.agent.WorkflowCodeEditorSpringAIAgent;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
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
public class WorkflowCodeEditorAgentConfiguration {

    private final Resource promptWorkflowCodeEditorAskResource;
    private final Resource promptWorkflowCodeEditorBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public WorkflowCodeEditorAgentConfiguration(
        @Value("classpath:prompt_workflow_code_editor_ask.txt") Resource promptWorkflowCodeEditorAskResource,
        @Value("classpath:prompt_workflow_code_editor_build.txt") Resource promptWorkflowCodeEditorBuildResource,
        WorkflowValidatorTools workflowValidatorTools,
        WorkflowInstructionTools workflowInstructionTools) {

        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptWorkflowCodeEditorAskResource = promptWorkflowCodeEditorAskResource;
        this.promptWorkflowCodeEditorBuildResource = promptWorkflowCodeEditorBuildResource;
    }

    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        WorkflowCodeEditorSpringAIAgent springAIAgent = WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(AutomationConfigurationAgentType.WORKFLOW_CODE_EDITOR_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    WorkflowCodeEditorSpringAIAgent workflowCodeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        WorkflowCodeEditorSpringAIAgent springAIAgent = WorkflowCodeEditorSpringAIAgent.builder()
            .agentId(AutomationConfigurationAgentType.WORKFLOW_CODE_EDITOR_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptWorkflowCodeEditorBuildResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
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
