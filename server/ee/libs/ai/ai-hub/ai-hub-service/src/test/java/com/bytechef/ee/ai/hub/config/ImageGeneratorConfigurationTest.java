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
import com.bytechef.ee.ai.hub.tool.GenerateImageToolCallback.ImageGenerationMeter;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageContext;
import com.bytechef.ee.platform.ai.tool.usage.ToolUsageRecorder;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

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
    void testImageGenerationMeterIsAbsentWithoutARecorder() {
        assertThat(ImageGeneratorConfiguration.imageGenerationMeter(null, chatService)).isNull();
    }

    @Test
    void testImageGenerationMeterRecordsAnOpenAiImageUsageRow() {
        when(chatService.findByThreadId("thread-1")).thenReturn(Optional.empty());

        ImageGenerationMeter imageGenerationMeter = ImageGeneratorConfiguration.imageGenerationMeter(
            toolUsageRecorder, chatService);

        assertThat(imageGenerationMeter).isNotNull();

        imageGenerationMeter.record(toolContext(), "1024x1792", 5L);

        verify(toolUsageRecorder).recordTool(
            eq(new ToolUsageContext(3L, 7L, 2, null)), eq("openai_image"), eq(1), eq(5L),
            eq(Map.of("size", "1024x1792")));
    }

    @Test
    void testImageGenerationMeterSkipsACallWithoutAWorkspace() {
        ImageGenerationMeter imageGenerationMeter = ImageGeneratorConfiguration.imageGenerationMeter(
            toolUsageRecorder, chatService);

        assertThat(imageGenerationMeter).isNotNull();

        imageGenerationMeter.record(null, "1024x1024", 5L);

        verify(toolUsageRecorder, never()).recordTool(any(), anyString(), eq(1), anyLong(), any());
    }

    @SuppressWarnings("unchecked")
    private AiHubSubAgentToolCallbackFactory newFactory(Resource promptResource) {
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider = mock(ObjectProvider.class);
        ObjectProvider<ToolUsageRecorder> toolUsageRecorderProvider = mock(ObjectProvider.class);

        return imageGeneratorConfiguration.imageGeneratorToolCallbackFactory(
            mock(ChatModel.class), chatModelResolverProvider, new ApplicationProperties(), mock(AssetFileFacade.class),
            mock(AiHubChatArtifactRecorder.class), toolUsageRecorderProvider, chatService, promptResource);
    }

    private static ToolContext toolContext() {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            3L, 7L, null, null, 2L, "thread-1");

        return new ToolContext(invocationContext.toToolContext());
    }
}
