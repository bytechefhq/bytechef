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

import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
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
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

/**
 * @author Nikolina Spehar
 */
class OpenRouterCreateSpeechActionTest {

    private final ArgumentCaptor<MediaType> mediaTypeArgumentCaptor = forClass(MediaType.class);
    private final RestClient.Builder mockedBuilder = mock(RestClient.Builder.class);
    private final byte[] mockedByteArray = new byte[] {
        1, 0
    };
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(INPUT, "input", MODEL, "model", RESPONSE_FORMAT, "pcm", VOICE, "voice", SPEED, 0.0,
            TOKEN, "token"));
    private final RequestBodySpec mockedRequestBodySpec = mock(RequestBodySpec.class);
    private final RequestBodyUriSpec mockedRequestBodyUriSpec = mock(RequestBodyUriSpec.class);
    private final ResponseSpec mockedResponseSpec = mock(ResponseSpec.class);
    private final RestClient mockedRestClient = mock(RestClient.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() {
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
            when(mockedResponseSpec.body(byte[].class))
                .thenReturn(mockedByteArray);

            when(mockedContext.file(any()))
                .thenReturn(mockedFileEntry);

            FileEntry result = OpenRouterCreateSpeechAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedFileEntry, result);

            assertEquals(
                List.of("https://openrouter.ai/api/v1", "Authorization", "Bearer token", "/audio/speech"),
                stringArgumentCaptor.getAllValues());

            assertEquals(MediaType.APPLICATION_JSON, mediaTypeArgumentCaptor.getValue());

            Map<String, Object> body = (Map<String, Object>) objectArgumentCaptor.getValue();

            assertEquals("input", body.get("input"));
            assertEquals("model", body.get("model"));
            assertEquals("voice", body.get("voice"));
            assertEquals("pcm", body.get("response_format"));
            assertEquals(0.0, body.get("speed"));
        }
    }
}
