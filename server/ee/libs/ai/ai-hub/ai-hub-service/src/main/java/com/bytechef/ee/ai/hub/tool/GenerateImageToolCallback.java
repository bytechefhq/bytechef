/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import com.bytechef.ai.agent.tool.ToolErrors;
import com.bytechef.config.ApplicationProperties;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring AI {@link ToolCallback} that generates images via OpenAI's image API and returns them as base64-encoded PNG
 * bytes.
 *
 * <p>
 * The tool caches an {@link OpenAiImageModel} instance (keyed by the property-configured API key) so TLS setup and
 * retry-template construction happen only once. The model name comes from
 * {@code bytechef.ai.provider.image.openai.options.model} (falling back to {@code dall-e-3}). Per-call state (prompt,
 * size, style) still builds fresh {@link OpenAiImageOptions}. The response format is always {@code b64_json} so the
 * caller receives raw bytes rather than a time-limited URL.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class GenerateImageToolCallback implements ToolCallback {

    private static final String DESCRIPTION =
        "Generate an image from a text prompt using the OpenAI image API. Returns the image as base64-encoded PNG bytes.";

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
                    }
                },
                "required": ["prompt"]
            }""";

    private final JsonMapper jsonMapper = new JsonMapper();
    @Nullable
    private final OpenAiImageModel imageModel;
    private final String imageModelName;

    public GenerateImageToolCallback(ApplicationProperties applicationProperties) {
        ApplicationProperties.Ai.Provider providerProperties = applicationProperties.getAi()
            .getProvider();

        String apiKey = providerProperties.getOpenAi()
            .getApiKey();

        this.imageModel = (apiKey == null || apiKey.isBlank()) ? null : buildImageModel(apiKey);

        String configModel = providerProperties.getImage()
            .getOpenAi()
            .getOptions()
            .getModel();

        this.imageModelName = configModel != null && !configModel.isBlank() ? configModel : "dall-e-3";
    }

    String getImageModelName() {
        return imageModelName;
    }

    private static OpenAiImageModel buildImageModel(String apiKey) {
        return new OpenAiImageModel(
            OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build());
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
            .name("generateImage")
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
            GenerateImageInput input = jsonMapper.readValue(toolInput, GenerateImageInput.class);

            if (input.prompt() == null || input.prompt()
                .isBlank()) {
                return toolError("prompt is required and must not be blank");
            }

            if (imageModel == null) {
                return toolError("OpenAI API key not configured");
            }

            String effectiveSize = input.size() != null ? input.size() : "1024x1024";
            int[] dimensions = parseDimensions(effectiveSize);

            String effectivePrompt = input.style() != null
                ? input.prompt() + " \u2014 " + input.style()
                : input.prompt();

            OpenAiImageOptions options = OpenAiImageOptions.builder()
                .model(imageModelName)
                .responseFormat("b64_json")
                .width(dimensions[0])
                .height(dimensions[1])
                .quality("standard")
                .build();

            ImagePrompt imagePrompt = new ImagePrompt(effectivePrompt, options);

            ImageResponse response = imageModel.call(imagePrompt);
            ImageGeneration result = response.getResult();
            Image output = result.getOutput();

            String b64Json = output.getB64Json();

            Map<String, Object> resultMap = new LinkedHashMap<>();

            resultMap.put("imageBytes", b64Json != null ? b64Json : "");
            resultMap.put("mimeType", "image/png");
            resultMap.put("prompt", input.prompt());
            resultMap.put("size", effectiveSize);

            return jsonMapper.writeValueAsString(resultMap);
        } catch (JacksonException exception) {
            return toolError("Invalid tool input: " + exception.getMessage());
        } catch (RuntimeException exception) {
            return toolError("Image generation failed: " + exception.getMessage());
        }
    }

    private int[] parseDimensions(String size) {
        String[] parts = size.split("x");

        if (parts.length == 2) {
            try {
                return new int[] {
                    Integer.parseInt(parts[0]), Integer.parseInt(parts[1])
                };
            } catch (NumberFormatException numberFormatException) {
                return new int[] {
                    1024, 1024
                };
            }
        }

        return new int[] {
            1024, 1024
        };
    }

    private String toolError(String message) {
        return ToolErrors.toolError(jsonMapper, message);
    }

    public record GenerateImageInput(String prompt, @Nullable String size, @Nullable String style) {
    }
}
