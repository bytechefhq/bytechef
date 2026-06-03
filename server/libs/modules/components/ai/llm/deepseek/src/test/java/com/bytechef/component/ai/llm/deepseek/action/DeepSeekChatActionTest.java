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

package com.bytechef.component.ai.llm.deepseek.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FREQUENCY_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.PRESENCE_PENALTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
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

import com.bytechef.component.ai.llm.ChatModel;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.web.client.RestClient;

/**
 * @author Nikolina Spehar
 */
class DeepSeekChatActionTest {

    private final RestClient.Builder mockRestClientBuilder = mock(RestClient.Builder.class);
    private final Parameters mockedConnectionParameters = MockParametersFactory.create(Map.of(TOKEN, "TOKEN"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<RestClient.Builder> restClientBuilderArgumentCaptor =
        forClass(RestClient.Builder.class);
    private MockedStatic<DeepSeekApi> deepSeekApiMockedStatic;
    private MockedStatic<ModelUtils> modelUtilsMockedStatic;

    @BeforeEach
    void beforeEach() {
        deepSeekApiMockedStatic = mockStatic(DeepSeekApi.class);
        modelUtilsMockedStatic = mockStatic(ModelUtils.class);

        modelUtilsMockedStatic.when(ModelUtils::getRestClientBuilder)
            .thenReturn(mockRestClientBuilder);

        DeepSeekApi.Builder mockedApiBuilder = mock(DeepSeekApi.Builder.class);
        DeepSeekApi mockedApi = mock(DeepSeekApi.class);

        deepSeekApiMockedStatic.when(DeepSeekApi::builder)
            .thenReturn(mockedApiBuilder);

        when(mockedApiBuilder.apiKey(stringArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.baseUrl(stringArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.restClientBuilder(restClientBuilderArgumentCaptor.capture()))
            .thenReturn(mockedApiBuilder);
        when(mockedApiBuilder.build())
            .thenReturn(mockedApi);
    }

    @AfterEach
    void afterEach() {
        assertEquals(List.of("TOKEN", "https://api.deepseek.com"), stringArgumentCaptor.getAllValues());
        assertEquals(mockRestClientBuilder, restClientBuilderArgumentCaptor.getValue());

        deepSeekApiMockedStatic.close();
        modelUtilsMockedStatic.close();
    }

    @Test
    void testCreateChatModelWithResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.JSON.name()),
                MODEL, "deepseek-chat",
                TEMPERATURE, 0.7,
                MAX_TOKENS, 1000,
                TOP_P, 0.9,
                FREQUENCY_PENALTY, 0.5,
                PRESENCE_PENALTY, 0.3,
                STOP, List.of("stop")));

        org.springframework.ai.chat.model.ChatModel chatModel = DeepSeekChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, true);

        assertNotNull(chatModel);
        assertInstanceOf(DeepSeekChatModel.class, chatModel);

        DeepSeekChatOptions options = (DeepSeekChatOptions) chatModel.getDefaultOptions();

        assertEquals("deepseek-chat", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(1000, options.getMaxTokens());
        assertEquals(0.9, options.getTopP());
        assertEquals(0.5, options.getFrequencyPenalty());
        assertEquals(0.3, options.getPresencePenalty());
        assertEquals(List.of("stop"), options.getStop());

        ResponseFormat responseFormat = options.getResponseFormat();

        assertNotNull(responseFormat);
        assertEquals(ResponseFormat.Type.JSON_OBJECT, responseFormat.getType());
    }

    @Test
    void testCreateChatModelWithoutResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "deepseek-chat",
                TEMPERATURE, 0.7,
                MAX_TOKENS, 1000,
                TOP_P, 0.9,
                FREQUENCY_PENALTY, 0.5,
                PRESENCE_PENALTY, 0.3,
                STOP, List.of("stop")));

        org.springframework.ai.chat.model.ChatModel chatModel = DeepSeekChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(DeepSeekChatModel.class, chatModel);

        DeepSeekChatOptions options = (DeepSeekChatOptions) chatModel.getDefaultOptions();

        assertEquals("deepseek-chat", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(1000, options.getMaxTokens());
        assertEquals(0.9, options.getTopP());
        assertEquals(0.5, options.getFrequencyPenalty());
        assertEquals(0.3, options.getPresencePenalty());
        assertEquals(List.of("stop"), options.getStop());
        assertNull(options.getResponseFormat());
    }
}
