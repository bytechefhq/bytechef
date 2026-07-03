/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResearchConfigurationTest {

    private final ResearchConfiguration researchConfiguration = new ResearchConfiguration();

    @Test
    @SuppressWarnings("unchecked")
    void testResearchChatClientIsBuilt() {
        ChatModel chatModel = mock(ChatModel.class);
        FirecrawlTools firecrawlTools = mock(FirecrawlTools.class);
        Resource promptResource = new ByteArrayResource(
            "You are a research assistant.".getBytes(StandardCharsets.UTF_8),
            "test prompt_research.txt");

        ObjectProvider<ToolUsageRecorder> usageRecorderProvider = mock(ObjectProvider.class);
        ObjectProvider<AiHubTaskService> taskServiceProvider = mock(ObjectProvider.class);
        JsonMapper jsonMapper = new JsonMapper();

        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                chatModel, firecrawlTools, usageRecorderProvider,
                taskServiceProvider, jsonMapper, promptResource));
    }

    @Test
    void testResearchToolCallbackIsNamedResearch() {
        ChatClient researchChatClient = mock(ChatClient.class);

        ToolCallback toolCallback = ResearchConfiguration.createResearchToolCallback(researchChatClient);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("research");
    }

    @Test
    void testResearchToolCallbackDescriptionMentionsReport() {
        ChatClient researchChatClient = mock(ChatClient.class);

        ToolCallback toolCallback = ResearchConfiguration.createResearchToolCallback(researchChatClient);

        String description = toolCallback.getToolDefinition()
            .description()
            .toLowerCase();

        assertThat(description).containsAnyOf("markdown", "report", "findings");
    }
}
