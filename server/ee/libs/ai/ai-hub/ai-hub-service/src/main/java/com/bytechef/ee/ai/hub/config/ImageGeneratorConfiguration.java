/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AiHubSubAgentToolCallbackFactory;
import com.bytechef.ee.ai.hub.tool.GenerateImageToolCallback;
import com.bytechef.ee.ai.hub.tool.GenerateImageToolCallback.ImageGenerationMeter;
import com.bytechef.ee.ai.hub.tool.ImageGeneratorToolCallback;
import com.bytechef.ee.ai.hub.usage.AiHubToolUsageContextResolver;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageContext;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnAiHub
public class ImageGeneratorConfiguration {

    static final String METERED_TOOL_NAME = "openai_image";

    @Bean
    AiHubSubAgentToolCallbackFactory imageGeneratorToolCallbackFactory(
        ChatModel chatModel, ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        ApplicationProperties applicationProperties, AssetFileFacade assetFileFacade,
        AiHubChatArtifactRecorder chatArtifactRecorder, ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider,
        AiHubChatService chatService, @Value("classpath:prompt_image_generator.txt") Resource promptResource) {

        IntelligentToolChatClientFactory chatClientFactory = SubAgentConfigurationSupport.chatClientFactory(
            chatModel, SubAgentConfigurationSupport.readPrompt(promptResource, "image generator"));
        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();
        List<ToolCallback> toolCallbacks = List.of(
            GenerateImageToolCallback.of(
                applicationProperties, assetFileFacade, chatArtifactRecorder,
                imageGenerationMeter(toolUsageRecorderProvider.getIfAvailable(), chatService)));

        return () -> new ImageGeneratorToolCallback(chatClientFactory, chatModelResolver, toolCallbacks);
    }

    static @Nullable ImageGenerationMeter imageGenerationMeter(
        @Nullable ToolUsageRecorder toolUsageRecorder, AiHubChatService chatService) {

        if (toolUsageRecorder == null) {
            return null;
        }

        Function<ToolContext, ToolUsageContext> contextResolver = AiHubToolUsageContextResolver.create(
            chatService, METERED_TOOL_NAME);

        return (toolContext, size, durationMs) -> {
            ToolUsageContext toolUsageContext = contextResolver.apply(toolContext);

            if (toolUsageContext == null) {
                return;
            }

            toolUsageRecorder.recordTool(toolUsageContext, METERED_TOOL_NAME, 1, durationMs, Map.of("size", size));
        };
    }
}
