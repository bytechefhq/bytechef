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

package com.bytechef.component.ai.llm.perplexity.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LOGIT_BIAS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.constant.LLMConstants.USER;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatModel.ResponseFormat;
import org.springframework.ai.openai.OpenAiChatOptions;

/**
 * @author Nikolina Spehar
 */
class PerplexityChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Duration> durationArgumentCaptor = forClass(Duration.class);
    private MockedStatic<OpenAIOkHttpClient> openAIOkHttpClientMockedStatic;

    @BeforeEach
    void beforeEach() {
        openAIOkHttpClientMockedStatic = mockStatic(OpenAIOkHttpClient.class);

        OpenAIOkHttpClient.Builder mockedClientBuilder = mock(OpenAIOkHttpClient.Builder.class);
        OpenAIClient mockedClient = mock(OpenAIClient.class);

        openAIOkHttpClientMockedStatic.when(OpenAIOkHttpClient::builder)
            .thenReturn(mockedClientBuilder);

        when(mockedClientBuilder.apiKey(stringArgumentCaptor.capture()))
            .thenReturn(mockedClientBuilder);
        when(mockedClientBuilder.baseUrl(stringArgumentCaptor.capture()))
            .thenReturn(mockedClientBuilder);
        when(mockedClientBuilder.timeout(durationArgumentCaptor.capture()))
            .thenReturn(mockedClientBuilder);
        when(mockedClientBuilder.build())
            .thenReturn(mockedClient);
    }

    @AfterEach
    void afterEach() {
        assertEquals(List.of("TOKEN", "https://api.perplexity.ai"), stringArgumentCaptor.getAllValues());
        assertEquals(Duration.ofMinutes(5), durationArgumentCaptor.getValue());

        openAIOkHttpClientMockedStatic.close();
    }

    @Test
    void testCreateChatModelWithResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.ofEntries(
                Map.entry(RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name())),
                Map.entry(MODEL, "sonar"), Map.entry(TEMPERATURE, 0.7), Map.entry(MAX_TOKENS, 1000),
                Map.entry(TOP_P, 0.9), Map.entry(FREQUENCY_PENALTY, 0.5), Map.entry(PRESENCE_PENALTY, 0.3),
                Map.entry(N, 1), Map.entry(STOP, List.of("stop")), Map.entry(USER, "user123"),
                Map.entry(LOGIT_BIAS, Map.of("50256", -100))));

        org.springframework.ai.chat.model.ChatModel chatModel = PerplexityChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, true);

        assertNotNull(chatModel);
        assertInstanceOf(OpenAiChatModel.class, chatModel);

        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("sonar", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(1000, options.getMaxTokens());
        assertEquals(0.9, options.getTopP());
        assertEquals(0.5, options.getFrequencyPenalty());
        assertEquals(0.3, options.getPresencePenalty());
        assertEquals(1, options.getN());
        assertEquals(List.of("stop"), options.getStop());
        assertEquals("user123", options.getUser());
        assertEquals(Map.of("50256", -100), options.getLogitBias());

        ResponseFormat responseFormat = options.getResponseFormat();

        assertNotNull(responseFormat);
        assertEquals(ResponseFormat.Type.TEXT, responseFormat.getType());
    }

    @Test
    void testCreateChatModelWithoutResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(MODEL, "sonar", TEMPERATURE, 0.7, MAX_TOKENS, 1000, TOP_P, 0.9, FREQUENCY_PENALTY, 0.5,
                PRESENCE_PENALTY, 0.3, N, 1, STOP, List.of("stop"), USER, "user123",
                LOGIT_BIAS, Map.of("50256", -100)));

        org.springframework.ai.chat.model.ChatModel chatModel = PerplexityChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(OpenAiChatModel.class, chatModel);

        OpenAiChatOptions options = (OpenAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("sonar", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(1000, options.getMaxTokens());
        assertEquals(0.9, options.getTopP());
        assertEquals(0.5, options.getFrequencyPenalty());
        assertEquals(0.3, options.getPresencePenalty());
        assertEquals(1, options.getN());
        assertEquals(List.of("stop"), options.getStop());
        assertEquals("user123", options.getUser());
        assertEquals(Map.of("50256", -100), options.getLogitBias());
        assertNull(options.getResponseFormat());
    }
}
