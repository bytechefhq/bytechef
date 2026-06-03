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

import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

/**
 * @author Nikolina Spehar
 */
class NanoGptCreateTranscriptionActionTest {

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Context.Http.Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ArgumentCaptor<MediaType> mediaTypeArgumentCaptor = forClass(MediaType.class);
    private final RestClient.Builder mockedBuilder = mock(RestClient.Builder.class);
    private final byte[] mockedByteArray = new byte[] {
        1, 1
    };
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final File mockedFile = mock(File.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE, mockedFileEntry, MODEL, "model", LANGUAGE, "hr", TOKEN, "token"));
    private final RequestBodySpec mockedRequestBodySpec = mock(RequestBodySpec.class);
    private final RequestBodyUriSpec mockedRequestBodyUriSpec = mock(RequestBodyUriSpec.class);
    private final ResponseSpec mockedResponseSpec = mock(ResponseSpec.class);
    private final RestClient mockedRestClient = mock(RestClient.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<FileEntry> fileEntryArgumentCaptor = forClass(FileEntry.class);

    @Test
    void testPerform() throws IOException {
        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, Context.Http.Executor> fn = fileFunctionArgumentCaptor.getValue();

                return fn.apply(mockedFile);
            });

        when(mockedFile.readAllBytes(fileEntryArgumentCaptor.capture()))
            .thenReturn(mockedByteArray);

        when(mockedFileEntry.getName())
            .thenReturn("fileName");

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
            when(mockedRequestBodySpec.retrieve())
                .thenReturn(mockedResponseSpec);
            when(mockedResponseSpec.body(any(ParameterizedTypeReference.class)))
                .thenReturn(Map.of("transcription", "transcription"));

            String result = NanoGptCreateTranscriptionAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals("transcription", result);
            assertNotNull(fileFunctionArgumentCaptor.getValue());
            assertEquals(mockedFileEntry, fileEntryArgumentCaptor.getValue());
            assertEquals(
                List.of("x-api-key", "token", "https://nano-gpt.com/api/transcribe"),
                stringArgumentCaptor.getAllValues());

            assertEquals(MediaType.MULTIPART_FORM_DATA, mediaTypeArgumentCaptor.getValue());

            ByteArrayResource expectedAudio = new ByteArrayResource(mockedByteArray) {
                @Override
                public String getFilename() {
                    return "fileName";
                }
            };

            assertEquals(
                Map.of("audio", List.of(expectedAudio), "model", List.of("model"), "language", List.of("hr")),
                objectArgumentCaptor.getValue());
        }
    }
}
