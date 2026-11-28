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
import com.bytechef.automation.configuration.tool.ReadProjectTools;
import com.bytechef.automation.configuration.tool.ReadProjectWorkflowTools;
import com.bytechef.platform.ai.skill.tool.ReadSkillsTools;
import com.bytechef.platform.ai.skill.tool.SkillAgentType;
import com.bytechef.platform.ai.skill.tool.SkillsTools;
import com.bytechef.platform.ai.skill.tool.agent.SkillsSpringAIAgent;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import com.bytechef.platform.ai.tool.WebSearchToolCallbackProvider;
import com.bytechef.platform.component.tool.ComponentTools;
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
public class SkillsAgentConfiguration {

    private final Resource promptSkillsAskResource;
    private final Resource promptSkillsBuildResource;
    private final WorkflowValidatorTools workflowValidatorTools;
    private final WorkflowInstructionTools workflowInstructionTools;
    private final State state = new State();
    private final String skillsAskSystemPrompt;
    private final String skillsBuildSystemPrompt;

    @SuppressFBWarnings({
        "EI", "CT_CONSTRUCTOR_THROW"
    })
    public SkillsAgentConfiguration(
        @Value("classpath:prompt_skills_ask.txt") Resource promptSkillsAskResource,
        @Value("classpath:prompt_skills_build.txt") Resource promptSkillsBuildResource,
        WorkflowValidatorTools workflowValidatorTools,
        WorkflowInstructionTools workflowInstructionTools) {

        this.workflowValidatorTools = workflowValidatorTools;
        this.workflowInstructionTools = workflowInstructionTools;
        this.promptSkillsAskResource = promptSkillsAskResource;
        this.promptSkillsBuildResource = promptSkillsBuildResource;
        this.skillsAskSystemPrompt = getSystemPrompt(promptSkillsAskResource);
        this.skillsBuildSystemPrompt = getSystemPrompt(promptSkillsBuildResource);
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
    SkillsSpringAIAgent skillsAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        WebSearchToolCallbackProvider webSearchToolCallbackProvider,
        SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        List<Object> tools = new ArrayList<>(
            List.of(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools));

        SkillsSpringAIAgent springAIAgent = SkillsSpringAIAgent.builder()
            .agentId(SkillAgentType.SKILLS_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsAskResource))
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, tools))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();

        springAIAgent.setWebSearchToolCallbackProvider(webSearchToolCallbackProvider);

        return springAIAgent;
    }

    @Bean
    SkillsSpringAIAgent skillsBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools, SecurityContextRehydrator securityContextRehydrator,
        ObjectProvider<OverrideChatClientResolver> overrideChatClientResolverProvider)
        throws AGUIException {

        return SkillsSpringAIAgent.builder()
            .agentId(SkillAgentType.SKILLS_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(getSystemPrompt(promptSkillsBuildResource))
            .state(state)
            .toolCallbacks(
                wrapTools(
                    securityContextRehydrator,
                    List.of(
                        skillsTools, readProjectTools, readProjectWorkflowTools, componentTools,
                        workflowValidatorTools, workflowInstructionTools)))
            .overrideChatClientResolver(overrideChatClientResolverProvider.getIfAvailable())
            .build();
    }

    @Bean
    public ChatClient skillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools) {

        return buildSkillsAskSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, readSkillsTools, List.of());
    }

    @Bean
    public IntelligentToolChatClientFactory skillsAskSubAgentChatClientFactory(
        @Qualifier("skillsAskSubAgentChatClient") ChatClient skillsAskSubAgentChatClient, ChatModel chatModel,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        ReadSkillsTools readSkillsTools, WebSearchToolCallbackProvider webSearchToolCallbackProvider) {

        return candidateChatModel -> {
            List<ToolCallback> webSearchToolCallbacks = getCurrentWebSearchToolCallbacks(
                webSearchToolCallbackProvider);

            if (candidateChatModel == null && webSearchToolCallbacks.isEmpty()) {
                return skillsAskSubAgentChatClient;
            }

            return buildSkillsAskSubAgentChatClient(
                candidateChatModel == null ? chatModel : candidateChatModel, readProjectTools, readProjectWorkflowTools,
                readSkillsTools, webSearchToolCallbacks);
        };
    }

    private ChatClient buildSkillsAskSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, ReadSkillsTools readSkillsTools,
        List<ToolCallback> webSearchToolCallbacks) {

        ChatClient.Builder builder = ChatClient.builder(chatModel)
            .defaultSystem(skillsAskSystemPrompt);

        List<Object> tools = new ArrayList<>(
            List.of(
                readSkillsTools, readProjectTools, readProjectWorkflowTools, workflowValidatorTools,
                workflowInstructionTools));

        builder.defaultTools(tools.toArray());

        builder.defaultToolCallbacks(webSearchToolCallbacks);

        return builder.build();
    }

    @Bean
    public ChatClient skillsBuildSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools) {

        return buildSkillsBuildSubAgentChatClient(
            chatModel, readProjectTools, readProjectWorkflowTools, skillsTools, componentTools);
    }

    @Bean
    public IntelligentToolChatClientFactory skillsBuildSubAgentChatClientFactory(
        @Qualifier("skillsBuildSubAgentChatClient") ChatClient skillsBuildSubAgentChatClient,
        ReadProjectTools readProjectTools, ReadProjectWorkflowTools readProjectWorkflowTools,
        SkillsTools skillsTools, ComponentTools componentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? skillsBuildSubAgentChatClient
            : buildSkillsBuildSubAgentChatClient(
                candidateChatModel, readProjectTools, readProjectWorkflowTools, skillsTools, componentTools);
    }

    private ChatClient buildSkillsBuildSubAgentChatClient(
        ChatModel chatModel, ReadProjectTools readProjectTools,
        ReadProjectWorkflowTools readProjectWorkflowTools, SkillsTools skillsTools,
        ComponentTools componentTools) {

        return ChatClient.builder(chatModel)
            .defaultSystem(skillsBuildSystemPrompt)
            .defaultTools(
                skillsTools, readProjectTools, readProjectWorkflowTools, componentTools, workflowValidatorTools,
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
