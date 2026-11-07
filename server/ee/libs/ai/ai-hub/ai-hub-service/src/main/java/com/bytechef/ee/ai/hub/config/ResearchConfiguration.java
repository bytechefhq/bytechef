/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.ResearchToolCallbackFactory;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnAiHub
public class ResearchConfiguration {

    @Bean
    ResearchToolCallbackFactory researchToolCallbackFactory(
        ChatModel chatModel, ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider, AiHubChatService chatService,
        JsonMapper jsonMapper, @Value("classpath:prompt_research.txt") Resource promptResource) {

        return new ResearchToolCallbackFactory(
            researchChatClientFactory(chatModel, readPrompt(promptResource)),
            chatModelResolverProvider.getIfAvailable(), toolUsageRecorderProvider.getIfAvailable(), chatService,
            jsonMapper);
    }

    static IntelligentToolChatClientFactory researchChatClientFactory(ChatModel chatModel, String systemPrompt) {
        return candidateChatModel -> ChatClient.builder(candidateChatModel == null ? chatModel : candidateChatModel)
            .defaultSystem(systemPrompt)
            .build();
    }

    private static String readPrompt(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read research prompt resource: " + resource.getDescription(), exception);
        }
    }
}
