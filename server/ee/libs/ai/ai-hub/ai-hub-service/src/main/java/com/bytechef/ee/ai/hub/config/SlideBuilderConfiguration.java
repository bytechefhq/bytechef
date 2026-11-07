/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.ai.tool.GetAssetFileContentToolCallback;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AiHubSubAgentToolCallbackFactory;
import com.bytechef.ee.ai.hub.tool.CreateSlideDeckToolCallback;
import com.bytechef.ee.ai.hub.tool.SlideBuilderToolCallback;
import com.bytechef.ee.ai.hub.usage.AiHubToolUsageContextResolver;
import com.bytechef.ee.platform.ai.tool.usage.MeteredToolCallback;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
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
public class SlideBuilderConfiguration {

    static final String METERED_TOOL_NAME = "pptx_generate";

    @Bean
    AiHubSubAgentToolCallbackFactory slideBuilderToolCallbackFactory(
        ChatModel chatModel, ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        AssetFileFacade assetFileFacade, AiHubChatArtifactRecorder chatArtifactRecorder,
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider, AiHubChatService chatService,
        JsonMapper jsonMapper, @Value("classpath:prompt_slide_builder.txt") Resource promptResource) {

        IntelligentToolChatClientFactory chatClientFactory = SubAgentConfigurationSupport.chatClientFactory(
            chatModel, SubAgentConfigurationSupport.readPrompt(promptResource, "slide builder"));
        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();
        List<ToolCallback> toolCallbacks = List.of(
            meter(
                new CreateSlideDeckToolCallback(assetFileFacade, chatArtifactRecorder),
                toolUsageRecorderProvider.getIfAvailable(), chatService, jsonMapper),
            new GetAssetFileContentToolCallback(assetFileFacade));

        return () -> new SlideBuilderToolCallback(chatClientFactory, chatModelResolver, toolCallbacks);
    }

    static ToolCallback meter(
        ToolCallback createSlideDeckToolCallback, @Nullable ToolUsageRecorder toolUsageRecorder,
        AiHubChatService chatService, JsonMapper jsonMapper) {

        if (toolUsageRecorder == null) {
            return createSlideDeckToolCallback;
        }

        return new MeteredToolCallback(
            createSlideDeckToolCallback, METERED_TOOL_NAME, 1, toolUsageRecorder,
            AiHubToolUsageContextResolver.create(chatService, METERED_TOOL_NAME),
            MeteredToolCallback.singleStringField("title"), jsonMapper);
    }
}
