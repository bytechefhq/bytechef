/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class SubAgentConfigurationSupport {

    private SubAgentConfigurationSupport() {
    }

    static IntelligentToolChatClientFactory chatClientFactory(ChatModel chatModel, String systemPrompt) {
        return candidateChatModel -> ChatClient.builder(candidateChatModel == null ? chatModel : candidateChatModel)
            .defaultSystem(systemPrompt)
            .build();
    }

    static String readPrompt(Resource resource, String subAgentName) {
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read " + subAgentName + " prompt resource: " + resource.getDescription(), exception);
        }
    }
}
