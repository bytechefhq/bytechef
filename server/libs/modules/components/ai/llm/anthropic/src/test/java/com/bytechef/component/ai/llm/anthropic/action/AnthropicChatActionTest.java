/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.llm.anthropic.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.REASONING_EFFORT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_SCHEMA;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.THINKING;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.ThinkingConfigParam;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;

/**
 * @author Nikolina Spehar
 */
class AnthropicChatActionTest {

    private static final String PRODUCT_SCHEMA = """
        {"type":"object","required":["result"],"properties":{"result":{"type":"array","items":{"type":"object",
        "properties":{"name":{"type":"string"},"price":{"type":"number","minimum":0}}}}}}
        """;

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateChatModelWithTemperature() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-3-5-sonnet-20241022", MAX_TOKENS, 1000, STOP, List.of("stop"), TOP_K, 50,
                TEMPERATURE, 0.7));

        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic = mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build())
                .thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build())
                .thenReturn(mock(AnthropicClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = AnthropicChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(AnthropicChatModel.class, chatModel);
            assertEquals(List.of("TOKEN", "TOKEN"), stringArgumentCaptor.getAllValues());

            AnthropicChatOptions anthropicChatOptions = ((AnthropicChatModel) chatModel).getOptions();

            assertEquals("claude-3-5-sonnet-20241022", anthropicChatOptions.getModel());
            assertEquals(1000, anthropicChatOptions.getMaxTokens());
            assertEquals(List.of("stop"), anthropicChatOptions.getStopSequences());
            assertEquals(50, anthropicChatOptions.getTopK());
            assertEquals(0.7, anthropicChatOptions.getTemperature());
            assertNull(anthropicChatOptions.getTopP());
            assertEquals(
                AnthropicCacheStrategy.CONVERSATION_HISTORY, anthropicChatOptions.getCacheOptions()
                    .getStrategy());
        }
    }

    @Test
    void testCreateChatModelWithTopP() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-3-5-sonnet-20241022", MAX_TOKENS, 1000, STOP, List.of("stop"), TOP_K, 50,
                TOP_P, 0.9));

        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic = mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build())
                .thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build())
                .thenReturn(mock(AnthropicClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = AnthropicChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(AnthropicChatModel.class, chatModel);

            AnthropicChatOptions anthropicChatOptions = ((AnthropicChatModel) chatModel).getOptions();

            assertEquals("claude-3-5-sonnet-20241022", anthropicChatOptions.getModel());
            assertEquals(1000, anthropicChatOptions.getMaxTokens());
            assertEquals(List.of("stop"), anthropicChatOptions.getStopSequences());
            assertEquals(50, anthropicChatOptions.getTopK());
            assertNull(anthropicChatOptions.getTemperature());
            assertEquals(0.9, anthropicChatOptions.getTopP());
            assertEquals(
                AnthropicCacheStrategy.CONVERSATION_HISTORY, anthropicChatOptions.getCacheOptions()
                    .getStrategy());
        }
    }

    @Test
    void testCreateChatModelWithNeitherTemperatureNorTopP() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-3-5-sonnet-20241022", MAX_TOKENS, 1000, STOP, List.of("stop"), TOP_K, 50));

        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic = mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build())
                .thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build()).thenReturn(mock(AnthropicClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = AnthropicChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(AnthropicChatModel.class, chatModel);

            AnthropicChatOptions anthropicChatOptions = ((AnthropicChatModel) chatModel).getOptions();

            assertEquals("claude-3-5-sonnet-20241022", anthropicChatOptions.getModel());
            assertEquals(1000, anthropicChatOptions.getMaxTokens());
            assertEquals(List.of("stop"), anthropicChatOptions.getStopSequences());
            assertEquals(50, anthropicChatOptions.getTopK());
            assertNull(anthropicChatOptions.getTemperature());
            assertNull(anthropicChatOptions.getTopP());
            assertEquals(
                AnthropicCacheStrategy.CONVERSATION_HISTORY, anthropicChatOptions.getCacheOptions()
                    .getStrategy());
        }
    }

    @Test
    void testCreateChatModelOmitsUnsetSamplingParameters() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-sonnet-5", MAX_TOKENS, 1000));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters);

        assertEquals("claude-sonnet-5", anthropicChatOptions.getModel());
        assertNull(anthropicChatOptions.getTopK());
        assertNull(anthropicChatOptions.getTemperature());
        assertNull(anthropicChatOptions.getTopP());
    }

    @Test
    void testCreateChatModelDisablesThinkingWhenThinkingIsOff() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-sonnet-5", MAX_TOKENS, 1000, THINKING, false));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters);

        ThinkingConfigParam thinking = anthropicChatOptions.getThinking();

        assertTrue(thinking.isDisabled());
    }

    @Test
    void testCreateChatModelDisablesThinkingWhenThinkingIsUnset() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-opus-5", MAX_TOKENS, 1000));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters);

        ThinkingConfigParam thinking = anthropicChatOptions.getThinking();

        assertTrue(thinking.isDisabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "claude-fable-5-1", "claude-fable-5", "claude-mythos-5-1", "claude-opus-5-5"
    })
    void testCreateChatModelOmitsThinkingForModelsThatAlwaysThink(String model) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, model, MAX_TOKENS, 1000, THINKING, false));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters);

        assertNull(anthropicChatOptions.getThinking());
    }

    @Test
    void testCreateChatModelUsesAdaptiveThinking() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-opus-5", MAX_TOKENS, 16000, THINKING, true, REASONING_EFFORT, "high"));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters);

        ThinkingConfigParam thinking = anthropicChatOptions.getThinking();

        assertTrue(thinking.isAdaptive());

        OutputConfig outputConfig = anthropicChatOptions.getOutputConfig();

        assertEquals(OutputConfig.Effort.HIGH, outputConfig.effort()
            .orElseThrow());
    }

    @Test
    void testCreateChatModelOmitsSamplingParametersWhenThinking() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-sonnet-5", MAX_TOKENS, 16000, THINKING, true, TOP_K, 50, TEMPERATURE, 0.7));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters);

        ThinkingConfigParam thinking = anthropicChatOptions.getThinking();

        assertTrue(thinking.isAdaptive());
        assertNull(anthropicChatOptions.getTopK());
        assertNull(anthropicChatOptions.getTemperature());

        OutputConfig outputConfig = anthropicChatOptions.getOutputConfig();

        assertEquals(OutputConfig.Effort.MEDIUM, outputConfig.effort()
            .orElseThrow());
    }

    @Test
    void testCreateChatModelSetsStrictOutputSchemaForStructuredData() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-haiku-4-5", MAX_TOKENS, 16000,
                RESPONSE, Map.of(RESPONSE_FORMAT, "JSON", RESPONSE_SCHEMA, PRODUCT_SCHEMA)));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters, true);

        Map<String, Object> schema = getOutputSchema(anthropicChatOptions);

        assertEquals("object", schema.get("type"));
        assertEquals(false, schema.get("additionalProperties"));

        Map<String, Object> item = getPath(schema, "properties", "result", "items");

        assertEquals(false, item.get("additionalProperties"));
        assertEquals(Map.of("type", "number"), getPath(item, "properties", "price"));
    }

    @Test
    void testCreateChatModelKeepsEffortAlongsideOutputSchema() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-opus-5", MAX_TOKENS, 16000, THINKING, true, REASONING_EFFORT, "high",
                RESPONSE, Map.of(RESPONSE_FORMAT, "JSON", RESPONSE_SCHEMA, PRODUCT_SCHEMA)));

        AnthropicChatOptions anthropicChatOptions = createChatOptions(mockedInputParameters, true);

        OutputConfig outputConfig = anthropicChatOptions.getOutputConfig();

        assertEquals(OutputConfig.Effort.HIGH, outputConfig.effort()
            .orElseThrow());
        assertTrue(outputConfig.format()
            .isPresent());
    }

    @Test
    void testCreateChatModelOmitsOutputSchemaForText() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-haiku-4-5", MAX_TOKENS, 1000, RESPONSE, Map.of(RESPONSE_FORMAT, "TEXT")));

        assertNull(createChatOptions(mockedInputParameters, true).getOutputConfig());
    }

    @Test
    void testCreateChatModelOmitsOutputSchemaWhenResponseFormatIsNotRequired() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-haiku-4-5", MAX_TOKENS, 1000,
                RESPONSE, Map.of(RESPONSE_FORMAT, "JSON", RESPONSE_SCHEMA, PRODUCT_SCHEMA)));

        assertNull(createChatOptions(mockedInputParameters, false).getOutputConfig());
    }

    @Test
    void testCreateChatModelOmitsOutputSchemaWhenResponseIsMissing() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "claude-haiku-4-5", MAX_TOKENS, 1000));

        assertNull(createChatOptions(mockedInputParameters, true).getOutputConfig());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "claude-3-haiku-20240307", "claude-3-5-sonnet-20241022", "claude-opus-4-0", "claude-opus-4-20250514",
        "claude-sonnet-4-0", "claude-sonnet-4-20250514"
    })
    void testCreateChatModelKeepsPromptOnlyStructuredOutputForLegacyModels(String model) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, model, MAX_TOKENS, 1000,
                RESPONSE, Map.of(RESPONSE_FORMAT, "JSON", RESPONSE_SCHEMA, PRODUCT_SCHEMA)));

        assertNull(createChatOptions(mockedInputParameters, true).getOutputConfig());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "claude-haiku-4-5", "claude-opus-4-1-20250805", "claude-sonnet-4-5-20250929", "claude-opus-4-8"
    })
    void testCreateChatModelUsesNativeStructuredOutputForSupportedModels(String model) {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, model, MAX_TOKENS, 1000,
                RESPONSE, Map.of(RESPONSE_FORMAT, "JSON", RESPONSE_SCHEMA, PRODUCT_SCHEMA)));

        OutputConfig outputConfig = createChatOptions(mockedInputParameters, true).getOutputConfig();

        assertTrue(outputConfig.format()
            .isPresent());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> getPath(Map<String, Object> schema, String... keys) {
        Map<String, Object> current = schema;

        for (String key : keys) {
            current = (Map<String, Object>) current.get(key);
        }

        return current;
    }

    private static Map<String, Object> getOutputSchema(AnthropicChatOptions anthropicChatOptions) {
        OutputConfig outputConfig = anthropicChatOptions.getOutputConfig();

        JsonOutputFormat jsonOutputFormat = outputConfig.format()
            .orElseThrow();

        JsonOutputFormat.Schema schema = jsonOutputFormat.schema();

        Map<String, Object> schemaMap = new HashMap<>();

        for (Map.Entry<String, JsonValue> entry : schema._additionalProperties()
            .entrySet()) {

            schemaMap.put(
                entry.getKey(), entry.getValue()
                    .convert(Object.class));
        }

        return schemaMap;
    }

    private AnthropicChatOptions createChatOptions(Parameters inputParameters) {
        return createChatOptions(inputParameters, false);
    }

    private AnthropicChatOptions createChatOptions(Parameters inputParameters, boolean responseFormatRequired) {
        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic = mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build())
                .thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture()))
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build())
                .thenReturn(mock(AnthropicClientAsync.class));

            AnthropicChatModel chatModel = (AnthropicChatModel) AnthropicChatAction.CHAT_MODEL.createChatModel(
                inputParameters, mockedConnectionParameters, responseFormatRequired);

            return chatModel.getOptions();
        }
    }
}
