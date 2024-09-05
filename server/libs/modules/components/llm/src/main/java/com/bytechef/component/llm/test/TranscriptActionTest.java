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

package com.bytechef.component.llm.test;

import static com.bytechef.component.llm.constant.LLMConstants.FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.llm.Transcript;
import java.net.MalformedURLException;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;

/**
 * @author Marko Kriskovic
 */
public abstract class TranscriptActionTest extends AbstractLLMActionTest {

    private static final String ANSWER = "ANSWER";

    protected void performTest(ActionDefinition.SingleConnectionPerformFunction perform) {
        try (MockedStatic<Transcript> mockedTranscript = Mockito.mockStatic(Transcript.class)) {
            mockedTranscript
                .when(() -> Transcript.getResponse(any(Transcript.class), eq(mockedParameters), eq(mockedParameters)))
                .thenReturn(ANSWER);

            String result = (String) perform.apply(mockedParameters, mockedParameters, mockedContext);

            assertEquals(ANSWER, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void getResponseTest(
        Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> mockedTranscriptModel)
        throws MalformedURLException {

        when(mockedParameters.getFileEntry(FILE))
            .thenReturn(new FileEntry() {
                @Override
                public String getExtension() {
                    return ".extension";
                }

                @Override
                public String getMimeType() {
                    return "mimeType";
                }

                @Override
                public String getName() {
                    return "Name";
                }

                @Override
                public String getUrl() {
                    return "http://testUrl";
                }
            });

        Transcript mockedTranscription = mock(Transcript.class);
        AudioTranscriptionResponse transcriptionResponse = new AudioTranscriptionResponse(
            new AudioTranscription(ANSWER));
        AudioTranscriptionResponse mockedTranscriptionResponse = spy(transcriptionResponse);

        when(mockedTranscription.createTranscriptionModel(mockedParameters, mockedParameters))
            .thenReturn(mockedTranscriptModel);
        when(mockedTranscriptModel.call(any(AudioTranscriptionPrompt.class))).thenReturn(mockedTranscriptionResponse);

        String response = Transcript.getResponse(mockedTranscription, mockedParameters, mockedParameters);

        assertEquals(ANSWER, response);
    }
}
