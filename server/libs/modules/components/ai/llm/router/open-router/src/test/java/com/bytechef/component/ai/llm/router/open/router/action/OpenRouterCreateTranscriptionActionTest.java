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

package com.bytechef.component.ai.llm.router.open.router.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

/**
 * @author Nikolina Spehar
 */
class OpenRouterCreateTranscriptionActionTest {

    private final ArgumentCaptor<MediaType> mediaTypeArgumentCaptor = forClass(MediaType.class);
    private final RestClient.Builder mockedBuilder = mock(RestClient.Builder.class);
    private final byte[] mockedByteArray = new byte[] {
        1, 0
    };
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE, mockedFileEntry, TEMPERATURE, 0.5, MODEL, "model", LANGUAGE, "language",
            TOKEN, "token"));
    private final RequestBodySpec mockedRequestBodySpec = mock(RequestBodySpec.class);
    private final RequestBodyUriSpec mockedRequestBodyUriSpec = mock(RequestBodyUriSpec.class);
    private final ResponseSpec mockedResponseSpec = mock(ResponseSpec.class);
    private final RestClient mockedRestClient = mock(RestClient.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
        when(mockedContext.file(any()))
            .thenReturn(mockedByteArray);
        when(mockedContext.encoder(any()))
            .thenReturn("base64data");

        when(mockedFileEntry.getExtension())
            .thenReturn("wav");

        try (MockedStatic<ModelUtils> modelUtilsMockedStatic = Mockito.mockStatic(ModelUtils.class)) {
            modelUtilsMockedStatic.when(ModelUtils::getRestClientBuilder)
                .thenReturn(mockedBuilder);

            when(mockedBuilder.baseUrl(stringArgumentCaptor.capture()))
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
            when(mockedRequestBodySpec.retrieve())
                .thenReturn(mockedResponseSpec);
            when(mockedResponseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(Map.of("text", "transcription"));

            String result = OpenRouterCreateTranscriptionAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals("transcription", result);
            assertEquals(
                List.of("https://openrouter.ai/api/v1", "Authorization", "Bearer token", "/audio/transcriptions"),
                stringArgumentCaptor.getAllValues());
            assertEquals(MediaType.APPLICATION_JSON, mediaTypeArgumentCaptor.getValue());
            assertEquals(
                Map.of("input_audio", Map.of("data", "base64data", "format", "wav"), "model", "model", "language",
                    "language", "temperature", 0.5),
                objectArgumentCaptor.getValue());
        }
    }
}
