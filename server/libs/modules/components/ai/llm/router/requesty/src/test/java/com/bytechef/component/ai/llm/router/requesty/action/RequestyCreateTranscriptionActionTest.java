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

package com.bytechef.component.ai.llm.router.requesty.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;

/**
 * @author Thibault Jaigu
 * @author Igor Beslic
 */
class RequestyCreateTranscriptionActionTest {

    private final ArgumentCaptor<MediaType> mediaTypeArgumentCaptor = forClass(MediaType.class);

    private final byte[] mockedByteArray = new byte[] {
        1, 0
    };
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FILE, mockedFileEntry, TEMPERATURE, 0.5, MODEL, "model", LANGUAGE, "language",
            TOKEN, "token"));
    private final Context.Http.Response mockedResponseSpec = mock(Context.Http.Response.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<Context.ContextFunction<Context.Http, Context.Http.Executor>> httpFunctionArgumentCaptor =
        forClass(Context.ContextFunction.class);

    @Test
    void testPerform() {
        when(mockedExecutor.execute()).thenReturn(mockedResponseSpec);

        when(mockedContext.http(httpFunctionArgumentCaptor.capture())).thenReturn(mockedExecutor);

        String result = RequestyCreateTranscriptionAction.perform(
            mockedParameters, mockedParameters, mockedContext);
        when(mockedContext.http(httpFunctionArgumentCaptor.capture())).thenReturn(mockedExecutor);

        assertEquals("transcription", result);
        assertEquals(
            List.of("https://router.requesty.ai/v1", "Authorization", "Bearer token", "/audio/transcriptions"),
            stringArgumentCaptor.getAllValues());
        assertEquals(MediaType.MULTIPART_FORM_DATA, mediaTypeArgumentCaptor.getValue());

        ByteArrayResource expectedAudio = new ByteArrayResource(mockedByteArray) {
            @Override
            public String getFilename() {
                return "speech.wav";
            }
        };

        assertEquals(
            Map.of(
                "file", List.of(expectedAudio), "model", List.of("model"), "language", List.of("language"),
                "temperature", List.of("0.5")),
            objectArgumentCaptor.getValue());

    }

    @Test
    void testPerformWithEmptyResponse() {
        when(mockedExecutor.execute()).thenReturn(mockedResponseSpec);

        when(mockedContext.http(httpFunctionArgumentCaptor.capture())).thenReturn(mockedExecutor);

        assertThrows(
            IllegalStateException.class,
            () -> RequestyCreateTranscriptionAction.perform(mockedParameters, mockedParameters, mockedContext));
    }
}
