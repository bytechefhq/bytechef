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

package com.bytechef.component.llm;

import static com.bytechef.component.llm.constant.LLMConstants.FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.net.MalformedURLException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscription;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;

/**
 * @author Marko Kriskovic
 */
public class TranscriptActionTest extends AbstractActionTest {

    private static final String ANSWER = "ANSWER";

    @Test
    public void testGetResponse()
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

        Transcript mockedTranscription = spy(new MockTranscript());
        Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> mockedTranscriptModel = mock(Model.class);

        when(mockedTranscription.createTranscriptionModel(mockedParameters, mockedParameters))
            .thenReturn(mockedTranscriptModel);

        AudioTranscriptionResponse transcriptionResponse = new AudioTranscriptionResponse(
            new AudioTranscription(ANSWER));

        AudioTranscriptionResponse mockedTranscriptionResponse = spy(transcriptionResponse);

        when(mockedTranscriptModel.call(any(AudioTranscriptionPrompt.class))).thenReturn(mockedTranscriptionResponse);

        String response = mockedTranscription.getResponse(mockedParameters, mockedParameters);

        assertEquals(ANSWER, response);
    }

    private static class MockTranscript implements Transcript {

        @Override
        public Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> createTranscriptionModel(
            Parameters inputParameters, Parameters connectionParameters) {
            return null;
        }
    }
}