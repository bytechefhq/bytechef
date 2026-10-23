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

import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

/**
 * @author Thibault Jaigu
 * @author Igor Beslic
 */
class RequestyCreateSpeechActionTest {

    private final ArgumentCaptor<MediaType> mediaTypeArgumentCaptor = forClass(MediaType.class);
    private final byte[] mockedByteArray = new byte[] {
        1, 0
    };
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(INPUT, "input", MODEL, "model", RESPONSE_FORMAT, "pcm", VOICE, "voice", SPEED, 0.0,
            TOKEN, "token"));
    private final Context.Http.Response mockedResponseSpec = mock(Context.Http.Response.class);
    private final ArgumentCaptor<Object> objectArgumentCaptor = forClass(Object.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<InputStream> inputStreamArgumentCaptor = forClass(InputStream.class);
    private final Executor mockedExecutor = mock(Executor.class);

    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<File, Executor>> fileFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final File mockedFile = mock(File.class);

    @Test
    void testPerform() throws IOException {

        when(mockedResponseSpec.getBody(byte[].class))
            .thenReturn(mockedByteArray);

        when(mockedExecutor.execute()).thenReturn(mockedResponseSpec);

        when(mockedContext.http(httpFunctionArgumentCaptor.capture())).thenReturn(mockedExecutor);

        when(mockedContext.file(fileFunctionArgumentCaptor.capture()))
            .thenAnswer(inv -> {
                ContextFunction<File, Executor> value = fileFunctionArgumentCaptor.getValue();

                return value.apply(mockedFile);
            });
        when(mockedFile.storeContent(stringArgumentCaptor.capture(), inputStreamArgumentCaptor.capture()))
            .thenReturn(mockedFileEntry);

        FileEntry result = RequestyCreateSpeechAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedFileEntry, result);

        assertEquals(
            List.of("https://router.requesty.ai/v1", "Authorization", "Bearer token", "/audio/speech",
                "speech.pcm"),
            stringArgumentCaptor.getAllValues());

        assertEquals(MediaType.APPLICATION_JSON, mediaTypeArgumentCaptor.getValue());
        InputStream inputStreamArgumentCaptorValue = inputStreamArgumentCaptor.getValue();

        assertInstanceOf(ByteArrayInputStream.class, inputStreamArgumentCaptorValue);
        assertArrayEquals(mockedByteArray, inputStreamArgumentCaptorValue.readAllBytes());
        assertEquals(
            Map.of("input", "input", "model", "model", "voice", "voice", "response_format", "pcm", SPEED, 0.0),
            objectArgumentCaptor.getValue());
    }
}
