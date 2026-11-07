/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.tool.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ee.platform.customcomponent.tool.CustomComponentAgentType;
import com.bytechef.ee.platform.customcomponent.tool.CustomComponentTools;
import com.bytechef.ee.platform.customcomponent.tool.ReadCustomComponentTools;
import com.bytechef.ee.platform.customcomponent.tool.agent.CustomComponentSpringAIAgent;
import com.bytechef.platform.ai.tool.RehydrateContextToolCallback;
import com.bytechef.platform.ai.tool.SecurityContextRehydrator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
@ConditionalOnExpression("${bytechef.ai.copilot.enabled:false} or ${bytechef.ai.hub.enabled:false}")
public class CustomComponentAgentConfiguration {

    private final String customComponentAskSystemPrompt;
    private final String customComponentBuildSystemPrompt;

    private final State state = new State();

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public CustomComponentAgentConfiguration(
        @Value("classpath:prompt_custom_component_ask.txt") Resource promptCustomComponentAskResource,
        @Value("classpath:prompt_custom_component_build.txt") Resource promptCustomComponentBuildResource) {

        this.customComponentAskSystemPrompt = readPrompt(promptCustomComponentAskResource);
        this.customComponentBuildSystemPrompt = readPrompt(promptCustomComponentBuildResource);
    }

    @Bean
    CustomComponentSpringAIAgent customComponentAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools,
        SecurityContextRehydrator securityContextRehydrator) throws AGUIException {

        return CustomComponentSpringAIAgent.builder()
            .agentId(CustomComponentAgentType.CUSTOM_COMPONENT_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(customComponentAskSystemPrompt)
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of(readCustomComponentTools)))
            .build();
    }

    @Bean
    CustomComponentSpringAIAgent customComponentBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools, SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        return CustomComponentSpringAIAgent.builder()
            .agentId(CustomComponentAgentType.CUSTOM_COMPONENT_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(customComponentBuildSystemPrompt)
            .state(state)
            .toolCallbacks(
                wrapTools(securityContextRehydrator, List.of(customComponentTools, readCustomComponentTools)))
            .build();
    }

    @Bean
    ChatClient customComponentAskSubAgentChatClient(
        ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools) {

        return buildCustomComponentAskSubAgentChatClient(chatModel, readCustomComponentTools);
    }

    @Bean
    IntelligentToolChatClientFactory customComponentAskSubAgentChatClientFactory(
        @Qualifier("customComponentAskSubAgentChatClient") ChatClient customComponentAskSubAgentChatClient,
        ReadCustomComponentTools readCustomComponentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? customComponentAskSubAgentChatClient
            : buildCustomComponentAskSubAgentChatClient(candidateChatModel, readCustomComponentTools);
    }

    private ChatClient buildCustomComponentAskSubAgentChatClient(
        ChatModel chatModel, ReadCustomComponentTools readCustomComponentTools) {

        return chatClientBuilder(chatModel)
            .defaultSystem(customComponentAskSystemPrompt)
            .defaultTools(readCustomComponentTools)
            .build();
    }

    @Bean
    ChatClient customComponentBuildSubAgentChatClient(
        ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools) {

        return buildCustomComponentBuildSubAgentChatClient(chatModel, customComponentTools, readCustomComponentTools);
    }

    @Bean
    IntelligentToolChatClientFactory customComponentBuildSubAgentChatClientFactory(
        @Qualifier("customComponentBuildSubAgentChatClient") ChatClient customComponentBuildSubAgentChatClient,
        CustomComponentTools customComponentTools, ReadCustomComponentTools readCustomComponentTools) {

        return candidateChatModel -> candidateChatModel == null
            ? customComponentBuildSubAgentChatClient
            : buildCustomComponentBuildSubAgentChatClient(
                candidateChatModel, customComponentTools, readCustomComponentTools);
    }

    private ChatClient buildCustomComponentBuildSubAgentChatClient(
        ChatModel chatModel, CustomComponentTools customComponentTools,
        ReadCustomComponentTools readCustomComponentTools) {

        return chatClientBuilder(chatModel)
            .defaultSystem(customComponentBuildSystemPrompt)
            .defaultTools(customComponentTools, readCustomComponentTools)
            .build();
    }

    private ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
        return ChatClient.builder(chatModel);
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

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read copilot agent prompt resource: " + resource.getDescription(), exception);
        }
    }
}
