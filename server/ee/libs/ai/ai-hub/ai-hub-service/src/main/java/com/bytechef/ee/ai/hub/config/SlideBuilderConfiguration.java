/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.automation.ai.tool.CreateBinaryAssetFileToolCallback;
import com.bytechef.automation.ai.tool.GetAssetFileContentToolCallback;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.task.AiHubTaskService;
import com.bytechef.ee.ai.hub.tool.AiHubTaskArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.CreateSlideDeckToolCallback;
import com.bytechef.ee.ai.hub.tool.OpenFileTabToolCallback;
import com.bytechef.ee.ai.hub.tool.SlideBuilderToolCallback;
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
 * Registers the {@code slideBuilderChatClient} Spring bean used by the ai_hub BUILD agent.
 *
 * <p>
 * The slide_builder subagent is a dedicated {@link ChatClient} pre-loaded with slide-deck-construction tools
 * ({@code createSlideDeck}, {@code createBinaryAssetFile}, {@code getAssetFileContent}, {@code openFileTab}) and the
 * {@code prompt_slide_builder.txt} system prompt. Its isolated context means the parent ai_hub BUILD agent never sees
 * the construction transcript — it only receives the final one-paragraph summary.
 *
 * <p>
 * The {@link SlideBuilderToolCallback} is intentionally <em>not</em> a Spring bean. It is instantiated inline in the
 * ai_hub BUILD agent bean method (via {@link #createSlideBuilderToolCallback}) so that it is registered only on that
 * agent. Other agents consume {@code ObjectProvider<ToolCallback>.orderedStream()} and must not receive the
 * slide_builder tool.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class SlideBuilderConfiguration {

    @Bean
    ChatClient slideBuilderChatClient(
        ChatModel chatModel,
        AssetFileFacade assetFileFacade,
        AiHubTaskArtifactRecorder taskArtifactRecorder,
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider,
        ObjectProvider<AiHubTaskService> taskServiceProvider,
        JsonMapper jsonMapper,
        @Value("classpath:prompt_slide_builder.txt") Resource promptResource) {

        String systemPrompt = readPrompt(promptResource);

        CreateSlideDeckToolCallback createSlideDeck = new CreateSlideDeckToolCallback();
        CreateBinaryAssetFileToolCallback createBinaryAssetFile =
            new CreateBinaryAssetFileToolCallback(assetFileFacade, taskArtifactRecorder, jsonMapper);
        GetAssetFileContentToolCallback getAssetFileContent = new GetAssetFileContentToolCallback(assetFileFacade);
        OpenFileTabToolCallback openFileTab = new OpenFileTabToolCallback();

        ToolUsageRecorder usageRecorder = toolUsageRecorderProvider.getIfAvailable();
        AiHubTaskService taskService = taskServiceProvider.getIfAvailable();

        ToolCallback meteredCreateSlideDeck = (usageRecorder != null && taskService != null)
            ? new MeteredToolCallback(
                createSlideDeck, "pptx_generate", 1, usageRecorder,
                AiHubToolUsageContextResolver.create(taskService, "pptx_generate"),
                MeteredToolCallback.singleStringField("title"), jsonMapper)
            : createSlideDeck;

        return ChatClient.builder(chatModel)
            .defaultSystem(systemPrompt)
            .defaultTools(
                meteredCreateSlideDeck, createBinaryAssetFile, getAssetFileContent, openFileTab)
            .build();
    }

    static SlideBuilderToolCallback createSlideBuilderToolCallback(ChatClient slideBuilderChatClient) {
        return new SlideBuilderToolCallback(slideBuilderChatClient);
    }

    private String readPrompt(Resource resource) {
        try {
            InputStream inputStream = resource.getInputStream();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Failed to read slide builder prompt resource: " + resource.getDescription(), exception);
        }
    }
}
