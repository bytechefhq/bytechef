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
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.tool.AiHubTaskArtifactRecorder;
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
class ImageGeneratorConfigurationTest {

    private final ImageGeneratorConfiguration imageGeneratorConfiguration = new ImageGeneratorConfiguration();

    @Test
    @SuppressWarnings("unchecked")
    void testImageGeneratorChatClientIsBuilt() {
        ChatModel chatModel = mock(ChatModel.class);
        ApplicationProperties applicationProperties = new ApplicationProperties();
        AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
        AiHubTaskArtifactRecorder artifactRecorder =
            mock(AiHubTaskArtifactRecorder.class);
        Resource promptResource = new ByteArrayResource(
            "You are an image-generation subagent.".getBytes(StandardCharsets.UTF_8),
            "test prompt_image_generator.txt");

        ObjectProvider<ToolUsageRecorder> usageRecorderProvider = mock(ObjectProvider.class);
        ObjectProvider<AiHubTaskService> taskServiceProvider = mock(ObjectProvider.class);

        JsonMapper jsonMapper = new JsonMapper();

        assertThatNoException().isThrownBy(
            () -> imageGeneratorConfiguration.imageGeneratorChatClient(
                chatModel, applicationProperties, assetFileFacade, artifactRecorder,
                usageRecorderProvider, taskServiceProvider, jsonMapper, promptResource));
    }

    @Test
    void testImageGeneratorToolCallbackIsNamedCorrectly() {
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);

        ToolCallback toolCallback =
            ImageGeneratorConfiguration.createImageGeneratorToolCallback(imageGeneratorChatClient);

        assertThat(toolCallback.getToolDefinition()
            .name()).isEqualTo("image_generator");
    }

    @Test
    void testImageGeneratorToolCallbackDescriptionMentionsImage() {
        ChatClient imageGeneratorChatClient = mock(ChatClient.class);

        ToolCallback toolCallback =
            ImageGeneratorConfiguration.createImageGeneratorToolCallback(imageGeneratorChatClient);

        String description = toolCallback.getToolDefinition()
            .description()
            .toLowerCase();

        assertThat(description).containsAnyOf("image", "visual", "generat");
    }
}
