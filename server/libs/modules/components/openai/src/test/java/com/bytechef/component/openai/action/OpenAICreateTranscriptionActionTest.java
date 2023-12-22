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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.hermes.component.definition.ActionContext;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.audio.TranscriptionResult;
import com.theokanning.openai.service.OpenAiService;
import java.io.File;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

/**
 * @author Monika Domiter
 */
public class OpenAICreateTranscriptionActionTest extends AbstractOpenAIActionTest {

    @Test
    public void testPerform() {
        TranscriptionResult transcriptionResult = Mockito.mock(TranscriptionResult.class);
        File file = Mockito.mock(File.class);
        ActionContext.FileEntry fileEntry = Mockito.mock(ActionContext.FileEntry.class);

        ArgumentCaptor<CreateTranscriptionRequest> createTranscriptionRequestArgumentCaptor =
            ArgumentCaptor.forClass(CreateTranscriptionRequest.class);
        ArgumentCaptor<File> fileArgumentCaptor = ArgumentCaptor.forClass(File.class);

        Mockito.when(parameterMap.getRequiredString(MODEL))
            .thenReturn("MODEL");
        Mockito.when(parameterMap.getString(PROMPT))
            .thenReturn("PROMPT");
        Mockito.when(parameterMap.getString(RESPONSE_FORMAT))
            .thenReturn("RESPONSE_FORMAT");
        Mockito.when(parameterMap.getDouble(TEMPERATURE))
            .thenReturn(0.0);
        Mockito.when(parameterMap.getRequiredFileEntry(FILE))
            .thenReturn(fileEntry);
        Mockito.when(context.file(any()))
            .thenReturn(file);

        try (MockedConstruction<OpenAiService> openAiServiceMockedConstruction =
            Mockito.mockConstruction(OpenAiService.class,
                (mock, context) -> when(mock.createTranscription(createTranscriptionRequestArgumentCaptor.capture(),
                    fileArgumentCaptor.capture())).thenReturn(transcriptionResult))) {

            TranscriptionResult perform = OpenAICreateTranscriptionAction.perform(parameterMap, parameterMap, context);

            Assertions.assertEquals(1, openAiServiceMockedConstruction.constructed()
                .size());
            Assertions.assertEquals(transcriptionResult, perform);

            OpenAiService mock = openAiServiceMockedConstruction.constructed()
                .get(0);

            verify(mock).createTranscription(createTranscriptionRequestArgumentCaptor.capture(),
                fileArgumentCaptor.capture());
            verify(mock, times(1)).createTranscription(createTranscriptionRequestArgumentCaptor.capture(),
                fileArgumentCaptor.capture());

            Assertions.assertEquals("MODEL", createTranscriptionRequestArgumentCaptor.getValue()
                .getModel());
            Assertions.assertEquals("PROMPT", createTranscriptionRequestArgumentCaptor.getValue()
                .getPrompt());
            Assertions.assertEquals("RESPONSE_FORMAT", createTranscriptionRequestArgumentCaptor.getValue()
                .getResponseFormat());
            Assertions.assertEquals(0.0, createTranscriptionRequestArgumentCaptor.getValue()
                .getTemperature());
        }
    }

}
