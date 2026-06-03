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

package com.bytechef.component.ai.llm.gemini.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.MAX_TOKENS;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.N;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.STOP;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_K;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TOP_P;
import static com.bytechef.component.ai.llm.gemini.constant.GeminiConstants.LOCATION;
import static com.bytechef.component.ai.llm.gemini.constant.GeminiConstants.PROJECT_ID;
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
import com.google.genai.Client;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

/**
 * @author Nikolina Spehar
 */
class GeminiChatActionTest {

    private final Parameters mockedConnectionParameters = MockParametersFactory.create(
        Map.of(PROJECT_ID, "test-project", LOCATION, "us-central1"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = forClass(Boolean.class);
    private MockedStatic<Client> clientMockedStatic;

    @BeforeEach
    void beforeEach() {
        clientMockedStatic = mockStatic(Client.class);

        Client.Builder mockedClientBuilder = mock(Client.Builder.class);
        Client mockedClient = mock(Client.class);

        clientMockedStatic.when(Client::builder)
            .thenReturn(mockedClientBuilder);

        when(mockedClientBuilder.project(stringArgumentCaptor.capture()))
            .thenReturn(mockedClientBuilder);
        when(mockedClientBuilder.location(stringArgumentCaptor.capture()))
            .thenReturn(mockedClientBuilder);
        when(mockedClientBuilder.vertexAI(booleanArgumentCaptor.capture()))
            .thenReturn(mockedClientBuilder);
        when(mockedClientBuilder.build())
            .thenReturn(mockedClient);
    }

    @AfterEach
    void afterEach() {
        assertEquals(List.of("test-project", "us-central1"), stringArgumentCaptor.getAllValues());
        assertEquals(true, booleanArgumentCaptor.getValue());

        clientMockedStatic.close();
    }

    @Test
    void testCreateChatModelWithResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.JSON.name()),
                MODEL, "gemini-2.0-flash",
                TEMPERATURE, 0.7,
                MAX_TOKENS, 1000,
                TOP_P, 0.9,
                TOP_K, 40,
                N, 1,
                STOP, List.of("stop")));

        org.springframework.ai.chat.model.ChatModel chatModel = GeminiChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, true);

        assertNotNull(chatModel);
        assertInstanceOf(GoogleGenAiChatModel.class, chatModel);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) chatModel.getOptions();

        assertEquals("gemini-2.0-flash", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(1000, options.getMaxOutputTokens());
        assertEquals(0.9, options.getTopP());
        assertEquals(40, options.getTopK());
        assertEquals(1, options.getCandidateCount());
        assertEquals(List.of("stop"), options.getStopSequences());
        assertEquals("application/json", options.getResponseMimeType());
    }

    @Test
    void testCreateChatModelWithTextResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                RESPONSE, Map.of(RESPONSE_FORMAT, ChatModel.ResponseFormat.TEXT.name()),
                MODEL, "gemini-2.0-flash",
                TEMPERATURE, 0.7,
                MAX_TOKENS, 1000,
                TOP_P, 0.9,
                TOP_K, 40,
                N, 1,
                STOP, List.of("stop")));

        org.springframework.ai.chat.model.ChatModel chatModel = GeminiChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, true);

        assertNotNull(chatModel);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("text/plain", options.getResponseMimeType());
    }

    @Test
    void testCreateChatModelWithoutResponseFormat() {
        Parameters mockedInputParameters = MockParametersFactory.create(
            Map.of(
                MODEL, "gemini-2.0-flash",
                TEMPERATURE, 0.7,
                MAX_TOKENS, 1000,
                TOP_P, 0.9,
                TOP_K, 40,
                N, 1,
                STOP, List.of("stop")));

        org.springframework.ai.chat.model.ChatModel chatModel = GeminiChatAction.CHAT_MODEL.createChatModel(
            mockedInputParameters, mockedConnectionParameters, false);

        assertNotNull(chatModel);
        assertInstanceOf(GoogleGenAiChatModel.class, chatModel);

        GoogleGenAiChatOptions options = (GoogleGenAiChatOptions) chatModel.getDefaultOptions();

        assertEquals("gemini-2.0-flash", options.getModel());
        assertEquals(0.7, options.getTemperature());
        assertEquals(1000, options.getMaxOutputTokens());
        assertEquals(0.9, options.getTopP());
        assertEquals(40, options.getTopK());
        assertEquals(1, options.getCandidateCount());
        assertEquals(List.of("stop"), options.getStopSequences());
        assertNull(options.getResponseMimeType());
    }
}
