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

package com.bytechef.component.ai.llm;

import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.net.MalformedURLException;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.model.Model;
import org.springframework.core.io.UrlResource;

/**
 * @author Marko Kriskovic
 */
public interface AudioTranscriptionModel {

    Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> createAudioTranscriptionModel(
        Parameters inputParameters, Parameters connectionParameters);

    default String getResponse(Parameters inputParameters, Parameters connectionParameters)
        throws MalformedURLException {

        Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> transcriptionModel = createAudioTranscriptionModel(
            inputParameters, connectionParameters);

        AudioTranscriptionPrompt transcriptionPrompt = getTranscriptionPrompt(inputParameters);

        AudioTranscriptionResponse response = transcriptionModel.call(transcriptionPrompt);

        org.springframework.ai.audio.transcription.AudioTranscription result = response.getResult();

        return result.getOutput();
    }

    private AudioTranscriptionPrompt getTranscriptionPrompt(Parameters inputParameters) throws MalformedURLException {
        FileEntry fileEntry = inputParameters.getFileEntry(FILE);

        return new AudioTranscriptionPrompt(new UrlResource(fileEntry.getUrl()));
    }
}
