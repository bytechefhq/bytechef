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
import com.bytechef.automation.configuration.tool.ReadProjectWorkflowTools;
import com.bytechef.automation.configuration.tool.ScriptTools;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.configuration.context.EnvironmentContext;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.workflow.tool.PlatformWorkflowAgentType;
import com.bytechef.platform.workflow.tool.WorkflowInstructionTools;
import com.bytechef.platform.workflow.tool.WorkflowValidatorTools;
import com.bytechef.platform.workflow.tool.agent.CodeEditorSpringAIAgent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
public class CodeEditorAgentConfiguration {

    private final Resource promptCodeEditorAskResource;
    private final Resource promptCodeEditorBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();
    private final String codeEditorAskSystemPrompt;
    private final String codeEditorBuildSystemPrompt;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public CodeEditorAgentConfiguration(
        @Value("classpath:prompt_code_editor_ask.txt") Resource promptCodeEditorAskResource,
        @Value("classpath:prompt_code_editor_build.txt") Resource promptCodeEditorBuildResource,
        WorkflowValidatorTools workflowValidatorTools,
        WorkflowInstructionTools workflowInstructionTools) {

        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptCodeEditorAskResource = promptCodeEditorAskResource;
        this.promptCodeEditorBuildResource = promptCodeEditorBuildResource;
        this.codeEditorAskSystemPrompt = getSystemPrompt(promptCodeEditorAskResource);
        this.codeEditorBuildSystemPrompt = getSystemPrompt(promptCodeEditorBuildResource);
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {
        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        CodeEditorSpringAIAgent springAIAgent = CodeEditorSpringAIAgent.builder()
            .agentId(PlatformWorkflowAgentType.CODE_EDITOR_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    CodeEditorSpringAIAgent codeEditorBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return CodeEditorSpringAIAgent.builder()
            .agentId(PlatformWorkflowAgentType.CODE_EDITOR_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptCodeEditorBuildResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                        workflowInstructionTools)))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
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
    public ChatClient codeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools) {

        return buildCodeEditorAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, List.of());
    }

    @Bean
    public IntelligentToolChatClientFactory codeEditorAskSubAgentChatClientFactory(
        @Qualifier("codeEditorAskSubAgentChatClient") ChatClient codeEditorAskSubAgentChatClient, ChatModel chatModel,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return codeEditorAskSubAgentChatClient;
            }

            return buildCodeEditorAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectWorkflowTools, componentTools,
                webSearchToolCallbacks);
        };
    }

    private ChatClient buildCodeEditorAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, List<ToolCallback> webSearchToolCallbacks) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(codeEditorAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(readProjectWorkflowTools, componentTools, workflowValidatorTools, workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    public ChatClient codeEditorBuildSubAgentChatClient(
        ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return buildCodeEditorBuildSubAgentChatClient(chatModel, scriptTools, readProjectWorkflowTools, componentTools);
    }

    @Bean
    public IntelligentToolChatClientFactory codeEditorBuildSubAgentChatClientFactory(
        @Qualifier("codeEditorBuildSubAgentChatClient") ChatClient codeEditorBuildSubAgentChatClient,
        ScriptTools scriptTools, ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeEditorBuildSubAgentChatClient
            : buildCodeEditorBuildSubAgentChatClient(
                candidateChatModel, scriptTools, readProjectWorkflowTools, componentTools);
    }

    private ChatClient buildCodeEditorBuildSubAgentChatClient(
        ChatModel chatModel, ScriptTools scriptTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(codeEditorBuildSystemPrompt)
            .defaultTools(
                readProjectWorkflowTools, scriptTools, componentTools, workflowValidatorTools,
                workflowInstructionTools)
            .build();
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
