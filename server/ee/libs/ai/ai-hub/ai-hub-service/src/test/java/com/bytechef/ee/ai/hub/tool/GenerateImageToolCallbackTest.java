/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageOptions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class GenerateImageToolCallbackTest {

    private static final byte[] IMAGE_BYTES = "png-bytes".getBytes(StandardCharsets.UTF_8);

    private final AiHubChatArtifactRecorder artifactRecorder = mock(AiHubChatArtifactRecorder.class);
    private final AssetFileFacade assetFileFacade = mock(AssetFileFacade.class);
    private final ImageModel imageModel = mock(ImageModel.class);
    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testCallGeneratesTheImageAndSavesItAsAWorkspaceFile() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse());
        when(assetFileFacade.createBinaryFromAi(
            eq(3L), eq(2), eq("hero-banner.png"), eq("image/png"), eq(IMAGE_BYTES), any(), any(), any(), any()))
                .thenReturn(assetFile());

        JsonNode node = jsonMapper.readTree(
            newCallback().call(
                "{\"prompt\":\"a sunrise\",\"size\":\"1792x1024\",\"style\":\"flat\",\"filename\":\"hero-banner\"}",
                toolContext()));

        assertThat(node.get("fileId")
            .asString()).isEqualTo("11");
        assertThat(node.get("name")
            .asString()).isEqualTo("hero-banner.png");
        assertThat(node.get("size")
            .asString()).isEqualTo("1792x1024");

        ArgumentCaptor<ImagePrompt> imagePromptCaptor = ArgumentCaptor.forClass(ImagePrompt.class);

        verify(imageModel).call(imagePromptCaptor.capture());

        ImagePrompt imagePrompt = imagePromptCaptor.getValue();

        OpenAiImageOptions options = (OpenAiImageOptions) imagePrompt.getOptions();

        assertThat(options.getModel()).isEqualTo("dall-e-3");
        assertThat(options.getWidth()).isEqualTo(1792);
        assertThat(options.getHeight()).isEqualTo(1024);
        assertThat(imagePrompt.getInstructions()
            .getFirst()
            .getText()).isEqualTo("a sunrise - flat");

        verify(artifactRecorder).record("thread-1", 7L, "FILE_CREATED", "11", "hero-banner.png");
    }

    @Test
    void testCallFallsBackToTheDefaultSizeForAnUnsupportedSize() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(imageResponse());
        when(assetFileFacade.createBinaryFromAi(any(), eq(2), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(assetFile());

        JsonNode node = jsonMapper.readTree(
            newCallback().call("{\"prompt\":\"a sunrise\",\"size\":\"640x480\"}", toolContext()));

        assertThat(node.get("size")
            .asString()).isEqualTo("1024x1024");
    }

    @Test
    void testCallReturnsErrorWithoutAnApiKey() {
        GenerateImageToolCallback callback = GenerateImageToolCallback.of(
            new ApplicationProperties(), assetFileFacade, artifactRecorder);

        assertThat(callback.call("{\"prompt\":\"a sunrise\"}", toolContext()))
            .contains("OpenAI API key not configured");
        assertThat(callback.getImageModelName()).isEqualTo("dall-e-3");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testCallReturnsErrorWithoutAWorkspace() {
        assertThat(newCallback().call("{\"prompt\":\"a sunrise\"}")).contains("Workspace context unavailable");

        verifyNoInteractions(imageModel, assetFileFacade);
    }

    @Test
    void testCallReturnsErrorWhenPromptIsBlank() {
        assertThat(newCallback().call("{\"prompt\":\" \"}", toolContext())).contains("prompt is required");

        verifyNoInteractions(imageModel);
    }

    @Test
    void testCallReturnsErrorWhenTheModelReturnsNoBytes() {
        when(imageModel.call(any(ImagePrompt.class))).thenReturn(
            new ImageResponse(List.of(new ImageGeneration(new Image("https://example.com/image.png", null)))));

        assertThat(newCallback().call("{\"prompt\":\"a sunrise\"}", toolContext())).contains("no image bytes");

        verifyNoInteractions(assetFileFacade);
    }

    @Test
    void testResolveFilenameAddsThePngExtension() {
        assertThat(GenerateImageToolCallback.resolveFilename("banner")).isEqualTo("banner.png");
        assertThat(GenerateImageToolCallback.resolveFilename("Banner.PNG")).isEqualTo("Banner.PNG");
        assertThat(GenerateImageToolCallback.resolveFilename(null)).startsWith("generated-image-")
            .endsWith(".png");
    }

    private GenerateImageToolCallback newCallback() {
        return new GenerateImageToolCallback(imageModel, "dall-e-3", assetFileFacade, artifactRecorder);
    }

    private static AssetFile assetFile() {
        AssetFile assetFile = new AssetFile();

        assetFile.setId(11L);
        assetFile.setName("hero-banner.png");

        return assetFile;
    }

    private static ImageResponse imageResponse() {
        return new ImageResponse(
            List.of(new ImageGeneration(new Image(null, EncodingUtils.base64EncodeToString(IMAGE_BYTES)))));
    }

    private static ToolContext toolContext() {
        AiHubToolInvocationContext invocationContext = new AiHubToolInvocationContext(
            3L, 7L, null, null, 2L, "thread-1");

        return new ToolContext(invocationContext.toToolContext());
    }
}
