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

import com.bytechef.platform.ai.tool.BraveWebSearchTools;
import com.bytechef.platform.ai.tool.FirecrawlTools;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
    void testResearchChatClientIsBuiltWithFirecrawlOnly() {
        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                mock(ChatModel.class), Optional.of(mock(FirecrawlTools.class)), Optional.empty(),
                mock(ObjectProvider.class), mock(ObjectProvider.class), new JsonMapper(), promptResource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResearchChatClientIsBuiltWithBraveOnly() {
        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                mock(ChatModel.class), Optional.empty(), Optional.of(mock(BraveWebSearchTools.class)),
                mock(ObjectProvider.class), mock(ObjectProvider.class), new JsonMapper(), promptResource()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testResearchChatClientIsBuiltWithBothProviders() {
        assertThatNoException().isThrownBy(
            () -> researchConfiguration.researchChatClient(
                mock(ChatModel.class), Optional.of(mock(FirecrawlTools.class)),
                Optional.of(mock(BraveWebSearchTools.class)), mock(ObjectProvider.class), mock(ObjectProvider.class),
                new JsonMapper(), promptResource()));
    }

    private static Resource promptResource() {
        return new ByteArrayResource(
            "You are a research assistant.".getBytes(StandardCharsets.UTF_8), "test prompt_research.txt");
    }

    @Test
    void testMapMeteredToolName() {
        assertThat(ResearchConfiguration.mapMeteredToolName("webSearch")).isEqualTo("firecrawl_search");
        assertThat(ResearchConfiguration.mapMeteredToolName("webpageScrape")).isEqualTo("firecrawl_scrape");
        assertThat(ResearchConfiguration.mapMeteredToolName("braveWebSearch")).isEqualTo("brave_search");
        assertThat(ResearchConfiguration.mapMeteredToolName("unknownTool")).isNull();
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
