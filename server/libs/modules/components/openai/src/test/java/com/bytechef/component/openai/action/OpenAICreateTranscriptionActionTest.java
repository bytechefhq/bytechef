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

import static com.bytechef.component.openai.constant.OpenAIConstants.FILE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.audio.TranscriptionResult;
import com.theokanning.openai.service.OpenAiService;
import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * @author Monika Domiter
 */
public class OpenAICreateTranscriptionActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        TranscriptionResult mockedTranscriptionResult = mock(TranscriptionResult.class);
        File mockedFile = mock(File.class);
        FileEntry mockedFileEntry = mock(FileEntry.class);

        ArgumentCaptor<CreateTranscriptionRequest> createTranscriptionRequestArgumentCaptor = ArgumentCaptor.forClass(
            CreateTranscriptionRequest.class);
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        when(mockedParameters.getRequiredString(MODEL))
            .thenReturn("MODEL");
        when(mockedParameters.getString(PROMPT))
            .thenReturn("PROMPT");
        when(mockedParameters.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        when(mockedParameters.getDouble(TEMPERATURE))
            .thenReturn(0.0);
        when(mockedParameters.getRequiredFileEntry(FILE))
            .thenReturn(mockedFileEntry);
        when(mockedContext.file(any()))
            .thenReturn(mockedFile);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction = mockConstruction(
            OpenAiService.class,
            (openAiService, context) -> when(openAiService.createTranscription(any(), any(File.class)))
                .thenReturn(mockedTranscriptionResult))) {

            TranscriptionResult transcriptionResult = OpenAICreateTranscriptionAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            List<OpenAiService> openAiServices = openAiServiceMockedConstruction.constructed();

            assertEquals(1, openAiServices.size());
            assertEquals(mockedTranscriptionResult, transcriptionResult);

            OpenAiService openAiService = openAiServices.getFirst();

            verify(openAiService, times(1)).createTranscription(
                createTranscriptionRequestArgumentCaptor.capture(), fileArgumentCaptor.capture());

            CreateTranscriptionRequest createTranscriptionRequest = createTranscriptionRequestArgumentCaptor.getValue();

            assertEquals("MODEL", createTranscriptionRequest.getModel());
            assertEquals("PROMPT", createTranscriptionRequest.getPrompt());
            assertEquals("RESPONSE_FORMAT", createTranscriptionRequest.getResponseFormat());
            assertEquals(0.0, createTranscriptionRequest.getTemperature());
        }
    }
}
