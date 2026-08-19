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

import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
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
class SlideBuilderConfigurationTest {

    private final SlideBuilderConfiguration slideBuilderConfiguration = new SlideBuilderConfiguration();

    @Test
    @SuppressWarnings("unchecked")
    void testSlideBuilderChatClientIsBuilt() {
        ChatModel chatModel = mock(ChatModel.class);
        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        AiHubChatArtifactRecorder artifactRecorder =
            mock(AiHubChatArtifactRecorder.class);
        Resource promptResource = new ByteArrayResource(
            "You are a slide-deck-construction subagent.".getBytes(StandardCharsets.UTF_8),
            "test prompt_slide_builder.txt");

        ObjectProvider<ToolUsageRecorder> usageRecorderProvider = mock(ObjectProvider.class);
        ObjectProvider<AiHubChatService> chatServiceProvider = mock(ObjectProvider.class);
        JsonMapper jsonMapper = new JsonMapper();

        assertThatNoException().isThrownBy(
            () -> slideBuilderConfiguration.slideBuilderChatClient(
                chatModel, assetFileFacade, artifactRecorder,
                usageRecorderProvider, chatServiceProvider, jsonMapper, promptResource));
    }

    @Test
    void testSlideBuilderToolCallbackIsNamedCorrectly() {
        ChatClient slideBuilderChatClient = mock(ChatClient.class);

        ToolCallback toolCallback = SlideBuilderConfiguration.createSlideBuilderToolCallback(slideBuilderChatClient);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("slide_builder");
    }

    @Test
    void testSlideBuilderToolCallbackDescriptionMentionsPresentation() {
        ChatClient slideBuilderChatClient = mock(ChatClient.class);

        ToolCallback toolCallback = SlideBuilderConfiguration.createSlideBuilderToolCallback(slideBuilderChatClient);

        String description = toolCallback.getToolDefinition()
            .description()
            .toLowerCase();

        assertThat(description).containsAnyOf("presentation", "pptx", "slide", "deck");
    }
}
