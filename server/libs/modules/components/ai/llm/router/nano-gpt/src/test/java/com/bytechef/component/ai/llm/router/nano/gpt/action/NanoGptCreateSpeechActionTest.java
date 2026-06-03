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

package com.bytechef.component.ai.llm.router.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;

/**
 * @author Nikolina Spehar
 */
class NanoGptCreateSpeechActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<InputStream> inputStreamArgumentCaptor = forClass(InputStream.class);
    private final ArgumentCaptor<MediaType> mediaTypeArgumentCaptor = forClass(MediaType.class);
    private final byte[] mockedAudioBytes = new byte[] {
        (byte) 0xFF, (byte) 0xFB, 0x10, 0x00
    };
    private final RestClient.Builder mockedBuilder = mock(RestClient.Builder.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            INPUT, "Hello world", MODEL, "tts-model", RESPONSE_FORMAT, "mp3", VOICE, "alloy",
            SPEED, 1.0, TOKEN, "test-api-key"));
    private final RequestBodySpec mockedRequestBodySpec = mock(RequestBodySpec.class);
    private final RequestBodyUriSpec mockedRequestBodyUriSpec = mock(RequestBodyUriSpec.class);
    private final RestClient mockedRestClient = mock(RestClient.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = Mockito.mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(ModelUtils::getRestClientBuilder)
                .thenReturn(mockedBuilder);

            when(mockedBuilder.defaultHeader(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedBuilder);
            when(mockedBuilder.build())
                .thenReturn(mockedRestClient);

            when(mockedRestClient.post())
                .thenReturn(mockedRequestBodyUriSpec);
            when(mockedRequestBodyUriSpec.uri(stringArgumentCaptor.capture()))
                .thenReturn(mockedRequestBodySpec);
            when(mockedRequestBodySpec.contentType(mediaTypeArgumentCaptor.capture()))
                .thenReturn(mockedRequestBodySpec);
            when(mockedRequestBodySpec.body(objectArgumentCaptor.capture()))
                .thenReturn(mockedRequestBodySpec);

            when(mockedRequestBodySpec.exchange(any()))
                .thenAnswer(inv -> {

                    org.springframework.http.client.ClientHttpResponse httpResponse =
                        mock(org.springframework.http.client.ClientHttpResponse.class);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
                    when(httpResponse.getHeaders()).thenReturn(headers);

                    InputStream bodyStream = new java.io.ByteArrayInputStream(mockedAudioBytes);
                    when(httpResponse.getBody()).thenReturn(bodyStream);

                    return mockedAudioBytes;
                });

            when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<File, Executor> fn = fileFunctionArgumentCaptor.getValue();
                    return fn.apply(mockedFile);
                });

            when(mockedFile.storeContent(stringArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()))
                .thenReturn(mockedFileEntry);

            Object rawResult = NanoGptCreateSpeechAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertInstanceOf(Map.class, rawResult);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) rawResult;

            assertEquals(mockedFileEntry, result.get("file"));
            assertNull(result.get("audioUrl"));

            String storedFilename = stringArgumentCaptor.getAllValues()
                .stream()
                .filter(s -> s.startsWith("speech."))
                .findFirst()
                .orElse(null);
            assertNotNull(storedFilename);
            assertEquals("speech.mp3", storedFilename);

            java.util.List<String> capturedStrings = stringArgumentCaptor.getAllValues();
            int apiKeyIndex = capturedStrings.indexOf("x-api-key");
            assertNotNull(apiKeyIndex >= 0 ? capturedStrings.get(apiKeyIndex + 1) : null,
                "API key header value should be present");
            assertEquals("test-api-key", capturedStrings.get(apiKeyIndex + 1));

            Object body = objectArgumentCaptor.getValue();
            assertInstanceOf(Map.class, body);

            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = (Map<String, Object>) body;

            assertEquals("Hello world", requestBody.get("text"));
            assertEquals("tts-model", requestBody.get("model"));
            assertEquals("alloy", requestBody.get("voice"));
            assertEquals("mp3", requestBody.get("response_format"));
            assertEquals(1.0, requestBody.get("speed"));

            assertEquals(MediaType.APPLICATION_JSON, mediaTypeArgumentCaptor.getValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPerformWithoutOptionalParams() throws IOException {
        Parameters minimalParameters = MockParametersFactory.create(
            Map.of(
                INPUT, "Minimal input",
                MODEL, "tts-model",
                TOKEN, "test-api-key"));

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = Mockito.mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(ModelUtils::getRestClientBuilder)
                .thenReturn(mockedBuilder);

            when(mockedBuilder.defaultHeader(any(), any()))
                .thenReturn(mockedBuilder);
            when(mockedBuilder.build())
                .thenReturn(mockedRestClient);

            when(mockedRestClient.post())
                .thenReturn(mockedRequestBodyUriSpec);
            when(mockedRequestBodyUriSpec.uri(any(String.class)))
                .thenReturn(mockedRequestBodySpec);
            when(mockedRequestBodySpec.contentType(any()))
                .thenReturn(mockedRequestBodySpec);
            when(mockedRequestBodySpec.body(objectArgumentCaptor.capture()))
                .thenReturn(mockedRequestBodySpec);

            when(mockedRequestBodySpec.exchange(any()))
                .thenReturn(mockedAudioBytes);

            when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<File, Executor> fn = fileFunctionArgumentCaptor.getValue();
                    return fn.apply(mockedFile);
                });

            when(mockedFile.storeContent(any(String.class), any(InputStream.class)))
                .thenReturn(mockedFileEntry);

            Object rawResult = NanoGptCreateSpeechAction.perform(minimalParameters, minimalParameters, mockedContext);

            assertInstanceOf(Map.class, rawResult);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) rawResult;

            assertEquals(mockedFileEntry, result.get("file"));

            Object body = objectArgumentCaptor.getValue();
            assertInstanceOf(Map.class, body);

            @SuppressWarnings("unchecked")
            Map<String, Object> requestBody = (Map<String, Object>) body;

            assertEquals("Minimal input", requestBody.get("text"));
            assertEquals("tts-model", requestBody.get("model"));
            assertEquals("mp3", requestBody.get("response_format"));
            assertNotNull(requestBody.containsKey("voice")
                ? null
                : "voice absent as expected");
        }
    }
}
