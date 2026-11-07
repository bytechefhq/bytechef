/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.ai.hub.chat.AiHubChatService;
import com.bytechef.ee.ai.hub.tool.AiHubChatArtifactRecorder;
import com.bytechef.ee.ai.hub.tool.AiHubSubAgentToolCallbackFactory;
import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.ee.platform.ai.tool.usage.MeteredToolCallback;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageContext;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ImageGeneratorConfigurationTest {

    private final AiHubChatService chatService = mock(AiHubChatService.class);
    private final ImageGeneratorConfiguration imageGeneratorConfiguration = new ImageGeneratorConfiguration();
    private final ToolUsageRecorder toolUsageRecorder = mock(ToolUsageRecorder.class);

    @Test
    void testImageGeneratorToolCallbackFactoryCreatesTheImageGeneratorTool() {
        AiHubSubAgentToolCallbackFactory factory = newFactory(new ClassPathResource("prompt_image_generator.txt"));

        assertThat(factory.create()
            .getToolDefinition()
            .name()).isEqualTo("image_generator");
    }

    @Test
    void testImageGeneratorToolCallbackFactoryFailsOnAMissingPrompt() {
        assertThatThrownBy(() -> newFactory(new ClassPathResource("missing_prompt_image_generator.txt")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("image generator prompt");
    }

    @Test
    void testMeterWrapsGenerateImageWhenARecorderIsPresent() {
        ToolCallback generateImageToolCallback = toolCallback("{\"fileId\":\"1\"}");

        assertThat(ImageGeneratorConfiguration.meter(
            generateImageToolCallback, toolUsageRecorder, chatService, new JsonMapper()))
                .isInstanceOf(MeteredToolCallback.class);
        assertThat(ImageGeneratorConfiguration.meter(generateImageToolCallback, null, chatService, new JsonMapper()))
            .isSameAs(generateImageToolCallback);
    }

    @Test
    void testMeteredGenerateImageRecordsAnOpenAiImageUsageRow() {
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.empty());

        ToolCallback meteredToolCallback = ImageGeneratorConfiguration.meter(
            toolCallback("{\"fileId\":\"1\"}"), toolUsageRecorder, chatService, new JsonMapper());

        meteredToolCallback.call("{\"prompt\":\"a sunrise\",\"size\":\"1024x1792\"}", toolContext());

        verify(toolUsageRecorder).recordTool(
            eq(new ToolUsageContext(3L, 7L, 2, null)), eq("openai_image"), eq(1), anyLong(),
            eq(Map.of("size", "1024x1792")));
    }

    @Test
    void testMeteredGenerateImageSkipsErrorResults() {
        ToolCallback meteredToolCallback = ImageGeneratorConfiguration.meter(
            toolCallback("{\"error\":\"OpenAI API key not configured\"}"), toolUsageRecorder, chatService,
            new JsonMapper());

        meteredToolCallback.call("{\"prompt\":\"a sunrise\"}", toolContext());

        verify(toolUsageRecorder, never()).recordTool(any(), anyString(), eq(1), anyLong(), any());
    }

    @SuppressWarnings("unchecked")
    private AiHubSubAgentToolCallbackFactory newFactory(Resource promptResource) {
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider = mock(ObjectProvider.class);
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider = mock(ObjectProvider.class);

        return imageGeneratorConfiguration.imageGeneratorToolCallbackFactory(
            mock(ChatModel.class), chatModelResolverProvider, new ApplicationProperties(), mock(AssetFileFacade.class),
            mock(AiHubChatArtifactRecorder.class), toolUsageRecorderProvider, chatService, new JsonMapper(),
            promptResource);
    }

    private static ToolCallback toolCallback(String result) {
        ToolCallback toolCallback = mock(ToolCallback.class);

        when(toolCallback.getToolDefinition()).thenReturn(
            ToolDefinition.builder()
                .name("generateImage")
                .description("Generate an image")
                .inputSchema("{}")
                .build());
        when(toolCallback.call(anyString(), any())).thenReturn(result);

        return toolCallback;
    }

    private static ToolContext toolContext() {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            3L, 7L, null, null, 2L, "thread-1");

        return new ToolContext(invocationContext.toToolContext());
    }
}
