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
import com.bytechef.automation.configuration.tool.ClusterElementTools;
import com.bytechef.automation.configuration.tool.ReadProjectWorkflowTools;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.tool.ComponentTools;
import com.bytechef.platform.component.tool.PlatformComponentAgentType;
import com.bytechef.platform.component.tool.TaskTools;
import com.bytechef.platform.component.tool.agent.ClusterElementSpringAIAgent;
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
public class ClusterElementAgentConfiguration {

    private final Resource promptClusterElementAskResource;
    private final Resource promptClusterElementBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();
    private final String clusterElementAskSystemPrompt;
    private final String clusterElementBuildSystemPrompt;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public ClusterElementAgentConfiguration(
        @Value("classpath:prompt_cluster_element_ask.txt") Resource promptClusterElementAskResource,
        @Value("classpath:prompt_cluster_element_build.txt") Resource promptClusterElementBuildResource,
        WorkflowValidatorTools workflowValidatorTools,
        WorkflowInstructionTools workflowInstructionTools) {

        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptClusterElementAskResource = promptClusterElementAskResource;
        this.promptClusterElementBuildResource = promptClusterElementBuildResource;
        this.clusterElementAskSystemPrompt = getSystemPrompt(promptClusterElementAskResource);
        this.clusterElementBuildSystemPrompt = getSystemPrompt(promptClusterElementBuildResource);
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider) throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        ClusterElementSpringAIAgent springAIAgent = ClusterElementSpringAIAgent.builder()
            .agentId(PlatformComponentAgentType.CLUSTER_ELEMENT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementAskResource))
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .state(state)
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    ClusterElementSpringAIAgent clusterElementBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return ClusterElementSpringAIAgent.builder()
            .agentId(PlatformComponentAgentType.CLUSTER_ELEMENT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptClusterElementBuildResource))
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        readProjectWorkflowTools, clusterElementTools, componentTools, taskTools,
                        workflowValidatorTools, workflowInstructionTools)))
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
    public ChatClient clusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return buildClusterElementAskSubAgentChatClient(
            chatModel, readProjectWorkflowTools, componentTools, taskTools, List.of());
    }

    @Bean
    public IntelligentToolChatClientFactory clusterElementAskSubAgentChatClientFactory(
        @Qualifier("clusterElementAskSubAgentChatClient") ChatClient clusterElementAskSubAgentChatClient,
        ChatModel chatModel,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return clusterElementAskSubAgentChatClient;
            }

            return buildClusterElementAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectWorkflowTools, componentTools,
                taskTools, webSearchToolCallbacks);
        };
    }

    private ChatClient buildClusterElementAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools, List<ToolCallback> webSearchToolCallbacks) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(clusterElementAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                readProjectWorkflowTools, componentTools, taskTools, workflowValidatorTools,
                workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    public ChatClient clusterElementBuildSubAgentChatClient(
        ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools) {

        return buildClusterElementBuildSubAgentChatClient(
            chatModel, clusterElementTools, readProjectWorkflowTools, componentTools, taskTools);
    }

    @Bean
    public IntelligentToolChatClientFactory clusterElementBuildSubAgentChatClientFactory(
        @Qualifier("clusterElementBuildSubAgentChatClient") ChatClient clusterElementBuildSubAgentChatClient,
        ClusterElementTools clusterElementTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ComponentTools componentTools, TaskTools taskTools) {

        return candidateChatModel -> candidateChatModel == null
            ? clusterElementBuildSubAgentChatClient
            : buildClusterElementBuildSubAgentChatClient(
                candidateChatModel, clusterElementTools, readProjectWorkflowTools, componentTools, taskTools);
    }

    private ChatClient buildClusterElementBuildSubAgentChatClient(
        ChatModel chatModel, ClusterElementTools clusterElementTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ComponentTools componentTools, TaskTools taskTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(clusterElementBuildSystemPrompt)
            .defaultTools(
                readProjectWorkflowTools, clusterElementTools, componentTools, taskTools, workflowValidatorTools,
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
