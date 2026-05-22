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
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.AnthropicClientAsync;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;

/**
 * @author Nikolina Spehar
 */
class AnthropicChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreateChatModelWithTemperature() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-3-5-sonnet-20241022",
                MAX_TOKENS, 1000,
                STOP, List.of("stop"),
                TOP_K, 50,
                TEMPERATURE, 0.7));

        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic =
                mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture())).thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build()).thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture())).thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build()).thenReturn(mock(AnthropicClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = AnthropicChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(AnthropicChatModel.class, chatModel);
            assertEquals(List.of("TOKEN", "TOKEN"), stringArgumentCaptor.getAllValues());

            AnthropicChatOptions options = ((AnthropicChatModel) chatModel).getOptions();

            assertEquals("claude-3-5-sonnet-20241022", options.getModel());
            assertEquals(1000, options.getMaxTokens());
            assertEquals(List.of("stop"), options.getStopSequences());
            assertEquals(50, options.getTopK());
            // temperature is set — topP must be null
            assertEquals(0.7, options.getTemperature());
            assertNull(options.getTopP());
        }
    }

    @Test
    void testCreateChatModelWithTopP() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-3-5-sonnet-20241022",
                MAX_TOKENS, 1000,
                STOP, List.of("stop"),
                TOP_K, 50,
                TOP_P, 0.9));

        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic =
                mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture())).thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build()).thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture())).thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build()).thenReturn(mock(AnthropicClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = AnthropicChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(AnthropicChatModel.class, chatModel);

            AnthropicChatOptions options = ((AnthropicChatModel) chatModel).getOptions();

            assertEquals("claude-3-5-sonnet-20241022", options.getModel());
            assertEquals(1000, options.getMaxTokens());
            assertEquals(List.of("stop"), options.getStopSequences());
            assertEquals(50, options.getTopK());
            // temperature is null — topP must be set
            assertNull(options.getTemperature());
            assertEquals(0.9, options.getTopP());
        }
    }

    @Test
    void testCreateChatModelWithNeitherTemperatureNorTopP() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "claude-3-5-sonnet-20241022",
                MAX_TOKENS, 1000,
                STOP, List.of("stop"),
                TOP_K, 50));

        try (MockedStatic<AnthropicOkHttpClient> syncMockedStatic = mockStatic(AnthropicOkHttpClient.class);
            MockedStatic<AnthropicOkHttpClientAsync> asyncMockedStatic =
                mockStatic(AnthropicOkHttpClientAsync.class)) {

            AnthropicOkHttpClient.Builder mockedSyncBuilder = mock(AnthropicOkHttpClient.Builder.class);

            syncMockedStatic.when(AnthropicOkHttpClient::builder)
                .thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.apiKey(stringArgumentCaptor.capture())).thenReturn(mockedSyncBuilder);
            when(mockedSyncBuilder.build()).thenReturn(mock(AnthropicClient.class));

            AnthropicOkHttpClientAsync.Builder mockedAsyncBuilder = mock(AnthropicOkHttpClientAsync.Builder.class);

            asyncMockedStatic.when(AnthropicOkHttpClientAsync::builder)
                .thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.apiKey(stringArgumentCaptor.capture())).thenReturn(mockedAsyncBuilder);
            when(mockedAsyncBuilder.build()).thenReturn(mock(AnthropicClientAsync.class));

            org.springframework.ai.chat.model.ChatModel chatModel = AnthropicChatAction.CHAT_MODEL.createChatModel(
                mockedInputParameters, mockedConnectionParameters, false);

            assertNotNull(chatModel);
            assertInstanceOf(AnthropicChatModel.class, chatModel);

            AnthropicChatOptions options = ((AnthropicChatModel) chatModel).getOptions();

            assertEquals("claude-3-5-sonnet-20241022", options.getModel());
            // neither set — both must be null
            assertNull(options.getTemperature());
            assertNull(options.getTopP());
        }
    }
}
