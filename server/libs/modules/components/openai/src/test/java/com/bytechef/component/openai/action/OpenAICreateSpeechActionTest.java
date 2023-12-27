/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.openai.action;

import static com.bytechef.component.openai.constant.OpenAIConstants.INPUT;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SPEED;
import static com.bytechef.component.openai.constant.OpenAIConstants.VOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext.FileEntry;
import com.theokanning.openai.audio.CreateSpeechRequest;
import com.theokanning.openai.service.OpenAiService;
import java.util.List;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * @author Monika Domiter
 */
public class OpenAICreateSpeechActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        ResponseBody mockedResponseBody = mock(ResponseBody.class);
        FileEntry mockedFileEntry = mock(FileEntry.class);
        ArgumentCaptor<CreateSpeechRequest> createSpeechRequestArgumentCaptor = ArgumentCaptor.forClass(
            CreateSpeechRequest.class);

        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("MODEL");
        when(mockedParameters.getRequiredString(INPUT))
            .thenReturn("INPUT");
        when(mockedParameters.getRequiredString(VOICE))
            .thenReturn("VOICE");
        when(mockedParameters.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        when(mockedParameters.getDouble(SPEED))
            .thenReturn(1.0);
        when(mockedContext.file(any()))
            .thenReturn(mockedFileEntry);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = mockConstruction(
            OpenAiService.class,
            (mock, context) -> when(mock.createSpeech(createSpeechRequestArgumentCaptor.capture()))
                .thenReturn(mockedResponseBody))) {

            FileEntry fileEntry = OpenAICreateSpeechAction.perform(mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(mockedFileEntry, fileEntry);

            OpenAiService mock = openAiServices.getFirst();

            verify(mock).createSpeech(createSpeechRequestArgumentCaptor.capture());
            verify(mock, times(1)).createSpeech(createSpeechRequestArgumentCaptor.capture());

            CreateSpeechRequest createSpeechRequest = createSpeechRequestArgumentCaptor.getValue();

            assertEquals("MODEL", createSpeechRequest.getModel());
            assertEquals("INPUT", createSpeechRequest.getInput());
            assertEquals("VOICE", createSpeechRequest.getVoice());
            assertEquals("RESPONSE_FORMAT", createSpeechRequest.getResponseFormat());
            assertEquals(1.0, createSpeechRequest.getSpeed());
        }
    }
}
