/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.automation.ai.tool.CreateBinaryAssetFileToolCallback;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.GenerateImageToolCallback;
import com.bytechef.ee.ai.hub.tool.ImageGeneratorToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenFileTabToolCallback;
import com.bytechef.ee.ai.hub.usage.AiHubToolUsageContextResolver;
import com.bytechef.ee.platform.ai.tool.usage.MeteredToolCallback;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registers the {@code imageGeneratorChatClient} Spring bean used by the ai_hub BUILD agent.
 *
 * <p>
 * The image_generator subagent is a dedicated {@link ChatClient} pre-loaded with image-generation tools
 * ({@code generateImage}, {@code createBinaryAssetFile}, {@code openFileTab}) and the
 * {@code prompt_image_generator.txt} system prompt. Its isolated context means the parent ai_hub BUILD agent never sees
 * the generation transcript — it only receives the final one-sentence summary.
 *
 * <p>
 * The {@link ImageGeneratorToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in the
 * ai_hub BUILD agent bean method (via {@link #createImageGeneratorToolCallback}) so that it is registered only on that
 * agent. Other agents consume {@code ObjectProvider<ToolCallback>.orderedStream()} and must not receive the
 * image_generator tool.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class ImageGeneratorConfiguration {

    @Bean
    ChatClient imageGeneratorChatClient(
        ChatModel chatModel,
        ApplicationProperties applicationProperties,
        AssetFileFacade assetFileFacade,
        AiHubChatArtifactRecorder chatArtifactRecorder,
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider,
        ObjectProvider<AiHubChatService> chatServiceProvider,
        JsonMapper jsonMapper,
        @Value("classpath:prompt_image_generator.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        GenerateImageToolCallback generateImage = new GenerateImageToolCallback(applicationProperties);
        CreateBinaryAssetFileToolCallback createBinaryAssetFile =
            new CreateBinaryAssetFileToolCallback(assetFileFacade, chatArtifactRecorder, jsonMapper);
        OpenFileTabToolCallback openFileTab = new OpenFileTabToolCallback();

        ToolUsageRecorder usageRecorder = toolUsageRecorderProvider.getIfAvailable();
        AiHubChatService chatService = chatServiceProvider.getIfAvailable();

        ToolCallback meteredGenerateImage = (usageRecorder != null && chatService != null)
            ? new MeteredToolCallback(
                generateImage, "openai_image", 1, usageRecorder,
                AiHubToolUsageContextResolver.create(chatService, "openai_image"),
                MeteredToolCallback.singleStringField("size"), jsonMapper)
            : generateImage;

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(meteredGenerateImage, createBinaryAssetFile, openFileTab)
            .build();
    }

    static ImageGeneratorToolCallback createImageGeneratorToolCallback(ChatClient imageGeneratorChatClient) {
        return new ImageGeneratorToolCallback(imageGeneratorChatClient);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read image generator prompt resource: " + resource.getDescription(), exception);
        }
    }
}
