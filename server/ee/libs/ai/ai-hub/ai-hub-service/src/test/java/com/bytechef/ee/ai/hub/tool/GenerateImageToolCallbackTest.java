/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Ai;
import com.bytechef.config.ApplicationProperties.Ai.Provider;
import com.bytechef.config.ApplicationProperties.Ai.Provider.OpenAi;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GenerateImageToolCallbackTest {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void testToolDefinitionExposesGenerateImageName() {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(buildProperties("sk-test-key"));

        ToolDefinition definition = callback.getToolDefinition();

        assertThat(definition.name()).isEqualTo("generateImage");
        assertThat(definition.description()).isNotBlank();
        assertThat(definition.inputSchema()).contains("prompt");
    }

    @Test
    void testCallReturnsErrorWhenApiKeyNotConfigured() throws Exception {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(buildProperties(null));

        String result = callback.call("{\"prompt\":\"a sunset over the ocean\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("OpenAI API key not configured");
    }

    @Test
    void testConstructionWithValidApiKeyCachesImageModel() throws Exception {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(buildProperties("sk-test-key"));

        Field imageModelField = GenerateImageToolCallback.class.getDeclaredField("imageModel");

        imageModelField.setAccessible(true);

        Object imageModel = imageModelField.get(callback);

        assertThat(imageModel).isNotNull()
            .isInstanceOf(OpenAiImageModel.class);
    }

    @Test
    void testImageModelNameComesFromConfiguration() {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(
            buildProperties("sk-test-key", "gpt-image-1"));

        assertThat(callback.getImageModelName()).isEqualTo("gpt-image-1");
    }

    @Test
    void testImageModelNameFallsBackWhenConfigurationBlank() {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(buildProperties("sk-test-key", " "));

        assertThat(callback.getImageModelName()).isEqualTo("dall-e-3");
    }

    @Test
    void testCallReturnsErrorOnBlankPrompt() throws Exception {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(buildProperties("sk-test-key"));

        String result = callback.call("{\"prompt\":\"\"}");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
        assertThat(node.get("error")
            .asText()).contains("prompt is required");
    }

    @Test
    void testCallReturnsErrorOnInvalidJson() throws Exception {
        GenerateImageToolCallback callback = new GenerateImageToolCallback(buildProperties("sk-test-key"));

        String result = callback.call("not-json");

        JsonNode node = jsonMapper.readTree(result);

        assertThat(node.has("error")).isTrue();
    }

    private ApplicationProperties buildProperties(String apiKeyValue) {
        return buildProperties(apiKeyValue, "dall-e-3");
    }

    private ApplicationProperties buildProperties(String apiKeyValue, String imageModelValue) {
        OpenAi openAi = mock(OpenAi.class);

        when(openAi.getApiKey()).thenReturn(apiKeyValue);

        Provider.Image.OpenAi.Options imageOptions = mock(Provider.Image.OpenAi.Options.class);

        when(imageOptions.getModel()).thenReturn(imageModelValue);

        Provider.Image.OpenAi imageOpenAi = mock(Provider.Image.OpenAi.class);

        when(imageOpenAi.getOptions()).thenReturn(imageOptions);

        Provider.Image image = mock(Provider.Image.class);

        when(image.getOpenAi()).thenReturn(imageOpenAi);

        Provider provider = mock(Provider.class);

        when(provider.getOpenAi()).thenReturn(openAi);
        when(provider.getImage()).thenReturn(image);

        Ai ai = mock(Ai.class);

        when(ai.getProvider()).thenReturn(provider);

        ApplicationProperties properties = mock(ApplicationProperties.class);

        when(properties.getAi()).thenReturn(ai);

        return properties;
    }
}
