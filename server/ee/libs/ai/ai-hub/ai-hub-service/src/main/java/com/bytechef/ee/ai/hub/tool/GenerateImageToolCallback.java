/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.automation.assetfile.domain.AssetFile;
import com.bytechef.automation.assetfile.service.AssetFileFacade;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GenerateImageToolCallback implements ToolCallback {

    static final String TOOL_NAME = "generateImage";

    private static final Logger log = LoggerFactory.getLogger(GenerateImageToolCallback.class);

    private static final String DEFAULT_IMAGE_MODEL_NAME = "dall-e-3";
    private static final String DEFAULT_SIZE = "1024x1024";
    private static final String MIME_TYPE = "image/png";
    private static final Set<String> SIZES = Set.of("1024x1024", "1024x1792", "1792x1024");

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static final String DESCRIPTION =
        """
            Generate an image from a text prompt with the OpenAI image API and save it as a PNG
            workspace file. Returns the saved file's fileId and name.""";

    private static final String INPUT_SCHEMA =
        """
            {
                "type": "object",
                "properties": {
                    "prompt": {
                        "type": "string",
                        "description": "What the image should depict"
                    },
                    "size": {
                        "type": "string",
                        "description": "Image size. Defaults to 1024x1024. Allowed: 1024x1024, 1024x1792, 1792x1024"
                    },
                    "style": {
                        "type": "string",
                        "description": "Optional style hint (e.g. photorealistic, illustration, minimal)"
                    },
                    "filename": {
                        "type": "string",
                        "description": "Descriptive filename ending in .png (e.g. auth-feature-banner.png)"
                    }
                },
                "required": ["prompt"]
            }""";

    private final @Nullable AiHubChatArtifactRecorder artifactRecorder;
    private final AssetFileFacade assetFileFacade;
    private final @Nullable ImageGenerationMeter imageGenerationMeter;
    private final @Nullable ImageModel imageModel;
    private final String imageModelName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    GenerateImageToolCallback(
        @Nullable ImageModel imageModel, String imageModelName, AssetFileFacade assetFileFacade,
        @Nullable AiHubChatArtifactRecorder artifactRecorder, @Nullable ImageGenerationMeter imageGenerationMeter) {

        this.artifactRecorder = artifactRecorder;
        this.assetFileFacade = assetFileFacade;
        this.imageGenerationMeter = imageGenerationMeter;
        this.imageModel = imageModel;
        this.imageModelName = imageModelName;
    }

    public static GenerateImageToolCallback of(
        ApplicationProperties applicationProperties, AssetFileFacade assetFileFacade,
        @Nullable AiHubChatArtifactRecorder artifactRecorder, @Nullable ImageGenerationMeter imageGenerationMeter) {

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Provider provider = ai.getProvider();

        ApplicationProperties.Ai.Provider.OpenAi openAi = provider.getOpenAi();

        String apiKey = openAi.getApiKey();

        ImageModel imageModel = apiKey == null || apiKey.isBlank()
            ? null
            : new OpenAiImageModel(
                OpenAIOkHttpClient.builder()
                    .apiKey(apiKey)
                    .build(),
                OpenAiImageOptions.builder()
                    .build());

        ApplicationProperties.Ai.Provider.Image image = provider.getImage();

        ApplicationProperties.Ai.Provider.Image.OpenAi imageOpenAi = image.getOpenAi();

        ApplicationProperties.Ai.Provider.Image.OpenAi.Options options = imageOpenAi.getOptions();

        String model = options.getModel();

        return new GenerateImageToolCallback(
            imageModel, model == null || model.isBlank() ? DEFAULT_IMAGE_MODEL_NAME : model, assetFileFacade,
            artifactRecorder, imageGenerationMeter);
    }

    String getImageModelName() {
        return imageModelName;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name(TOOL_NAME)
            .description(DESCRIPTION)
            .inputSchema(INPUT_SCHEMA)
            .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        try {
            GenerateImageInput input = JsonUtils.read(toolInput, GenerateImageInput.class);

            String prompt = input.prompt();

            if (prompt == null || prompt.isBlank()) {
                return ToolErrors.toolError("prompt is required and must not be blank");
            }

            if (imageModel == null) {
                return ToolErrors.toolError("OpenAI API key not configured");
            }

            AiHubToolInvocationContext invocationContext = AiHubToolInvocationContext.fromToolContext(toolContext);

            if (invocationContext == null || invocationContext.workspaceId() == null) {
                return ToolErrors.toolError("Workspace context unavailable - open this chat from a workspace.");
            }

            String requestedSize = input.size();

            String size = requestedSize != null && SIZES.contains(requestedSize) ? requestedSize : DEFAULT_SIZE;

            String[] dimensions = size.split("x");

            String style = input.style();

            OpenAiImageOptions options = OpenAiImageOptions.builder()
                .model(imageModelName)
                .responseFormat("b64_json")
                .width(Integer.parseInt(dimensions[0]))
                .height(Integer.parseInt(dimensions[1]))
                .quality("standard")
                .build();

            long startNanos = System.nanoTime();

            ImageResponse response = imageModel.call(
                new ImagePrompt(style == null || style.isBlank() ? prompt : prompt + " - " + style, options));

            recordGeneration(toolContext, size, (System.nanoTime() - startNanos) / 1_000_000L);

            ImageGeneration result = response.getResult();

            if (result == null) {
                return ToolErrors.toolError("Image generation returned no result");
            }

            Image output = result.getOutput();

            String b64Json = output.getB64Json();

            if (b64Json == null || b64Json.isBlank()) {
                return ToolErrors.toolError("Image generation returned no image bytes");
            }

            AssetFile assetFile = assetFileFacade.createBinaryFromAi(
                invocationContext.workspaceId(),
                AiHubToolInvocationContext.resolveEnvironmentOrDefault(invocationContext),
                resolveFilename(input.filename()), MIME_TYPE, EncodingUtils.base64Decode(b64Json), null, null,
                invocationContext.sourceOrdinal(), invocationContext.lastUserPrompt());

            String fileId = String.valueOf(assetFile.getId());

            if (artifactRecorder != null && invocationContext.threadId() != null) {
                artifactRecorder.record(
                    invocationContext.threadId(), invocationContext.userId(), "FILE_CREATED", fileId,
                    assetFile.getName());
            }

            Map<String, Object> resultMap = new LinkedHashMap<>();

            resultMap.put("fileId", fileId);
            resultMap.put("name", assetFile.getName());
            resultMap.put("mimeType", MIME_TYPE);
            resultMap.put("size", size);

            return JsonUtils.write(resultMap);
        } catch (JacksonException exception) {
            return ToolErrors.invalidInput(exception);
        } catch (RuntimeException exception) {
            return ToolErrors.runtimeFailure(GenerateImageToolCallback.class, TOOL_NAME, exception);
        }
    }

    private void recordGeneration(@Nullable ToolContext toolContext, String size, long durationMs) {
        if (imageGenerationMeter == null) {
            return;
        }

        try {
            imageGenerationMeter.record(toolContext, size, durationMs);
        } catch (RuntimeException exception) {
            log.warn("Failed to record the usage of a generated image: {}", exception.getMessage(), exception);
        }
    }

    static String resolveFilename(@Nullable String filename) {
        if (filename == null || filename.isBlank()) {
            return "generated-image-" + LocalDateTime.now()
                .format(FILENAME_FORMATTER) + ".png";
        }

        String trimmedFilename = filename.trim();

        return trimmedFilename.toLowerCase(Locale.ROOT)
            .endsWith(".png") ? trimmedFilename : trimmedFilename + ".png";
    }

    @FunctionalInterface
    public interface ImageGenerationMeter {

        void record(@Nullable ToolContext toolContext, String size, long durationMs);
    }

    public record GenerateImageInput(
        @Nullable String prompt, @Nullable String size, @Nullable String style, @Nullable String filename) {
    }
}
