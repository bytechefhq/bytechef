/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.tool.config;

import com.agui.core.exception.AGUIException;
import com.agui.core.state.State;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ee.automation.configuration.tool.CodeWorkflowAgentType;
import com.bytechef.ee.automation.configuration.tool.CodeWorkflowTools;
import com.bytechef.ee.automation.configuration.tool.ReadCodeWorkflowTools;
import com.bytechef.ee.automation.configuration.tool.agent.CodeWorkflowSpringAIAgent;
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
public class CodeWorkflowAgentConfiguration {

    private final String codeWorkflowAskSystemPrompt;
    private final String codeWorkflowBuildSystemPrompt;

    private final State state = new State();

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public CodeWorkflowAgentConfiguration(
        @Value("classpath:prompt_code_workflow_ask.txt") Resource promptCodeWorkflowAskResource,
        @Value("classpath:prompt_code_workflow_build.txt") Resource promptCodeWorkflowBuildResource) {

        this.codeWorkflowAskSystemPrompt = readPrompt(promptCodeWorkflowAskResource);
        this.codeWorkflowBuildSystemPrompt = readPrompt(promptCodeWorkflowBuildResource);
    }

    @Bean
    CodeWorkflowSpringAIAgent codeWorkflowAskSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, ReadCodeWorkflowTools readCodeWorkflowTools,
        SecurityContextRehydrator securityContextRehydrator) throws AGUIException {

        return CodeWorkflowSpringAIAgent.builder()
            .agentId(CodeWorkflowAgentType.CODE_WORKFLOW_ASK.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(codeWorkflowAskSystemPrompt)
            .state(state)
            .toolCallbacks(wrapTools(securityContextRehydrator, List.of(readCodeWorkflowTools)))
            .build();
    }

    @Bean
    CodeWorkflowSpringAIAgent codeWorkflowBuildSpringAIAgent(
        ChatMemory chatMemory, ChatModel chatModel, CodeWorkflowTools codeWorkflowTools,
        ReadCodeWorkflowTools readCodeWorkflowTools, SecurityContextRehydrator securityContextRehydrator)
        throws AGUIException {

        return CodeWorkflowSpringAIAgent.builder()
            .agentId(CodeWorkflowAgentType.CODE_WORKFLOW_BUILD.key())
            .chatMemory(chatMemory)
            .chatModel(chatModel)
            .systemMessage(codeWorkflowBuildSystemPrompt)
            .state(state)
            .toolCallbacks(
                wrapTools(securityContextRehydrator, List.of(codeWorkflowTools, readCodeWorkflowTools)))
            .build();
    }

    @Bean
    ChatClient codeWorkflowAskSubAgentChatClient(ChatModel chatModel, ReadCodeWorkflowTools readCodeWorkflowTools) {
        return buildCodeWorkflowAskSubAgentChatClient(chatModel, readCodeWorkflowTools);
    }

    @Bean
    IntelligentToolChatClientFactory codeWorkflowAskSubAgentChatClientFactory(
        @Qualifier("codeWorkflowAskSubAgentChatClient") ChatClient codeWorkflowAskSubAgentChatClient,
        ReadCodeWorkflowTools readCodeWorkflowTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeWorkflowAskSubAgentChatClient
            : buildCodeWorkflowAskSubAgentChatClient(candidateChatModel, readCodeWorkflowTools);
    }

    private ChatClient buildCodeWorkflowAskSubAgentChatClient(
        ChatModel chatModel, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return chatClientBuilder(chatModel)
            .defaultSystem(codeWorkflowAskSystemPrompt)
            .defaultTools(readCodeWorkflowTools)
            .build();
    }

    @Bean
    ChatClient codeWorkflowBuildSubAgentChatClient(
        ChatModel chatModel, CodeWorkflowTools codeWorkflowTools, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return buildCodeWorkflowBuildSubAgentChatClient(chatModel, codeWorkflowTools, readCodeWorkflowTools);
    }

    @Bean
    IntelligentToolChatClientFactory codeWorkflowBuildSubAgentChatClientFactory(
        @Qualifier("codeWorkflowBuildSubAgentChatClient") ChatClient codeWorkflowBuildSubAgentChatClient,
        CodeWorkflowTools codeWorkflowTools, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return candidateChatModel -> candidateChatModel == null
            ? codeWorkflowBuildSubAgentChatClient
            : buildCodeWorkflowBuildSubAgentChatClient(candidateChatModel, codeWorkflowTools, readCodeWorkflowTools);
    }

    private ChatClient buildCodeWorkflowBuildSubAgentChatClient(
        ChatModel chatModel, CodeWorkflowTools codeWorkflowTools, ReadCodeWorkflowTools readCodeWorkflowTools) {

        return chatClientBuilder(chatModel)
            .defaultSystem(codeWorkflowBuildSystemPrompt)
            .defaultTools(codeWorkflowTools, readCodeWorkflowTools)
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
