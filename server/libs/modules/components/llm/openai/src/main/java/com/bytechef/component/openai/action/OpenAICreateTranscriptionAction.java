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

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static constants.LLMConstants.CREATE_TRANSCRIPTION;
import static constants.LLMConstants.FILE;
import static constants.LLMConstants.LANGUAGE;
import static constants.LLMConstants.LANGUAGE_PROPERTY;
import static constants.LLMConstants.MODEL;
import static constants.LLMConstants.PROMPT;
import static constants.LLMConstants.RESPONSE_FORMAT;
import static constants.LLMConstants.TEMPERATURE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.core.io.UrlResource;
import util.LLMUtils;

/**
 * @author Monika Domiter
 */
public class OpenAICreateTranscriptionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TRANSCRIPTION)
        .title("Create transcriptions")
        .description("Transcribes audio into the input language.")
        .properties(
            fileEntry(FILE)
                .label("File")
                .description("The audio file object to transcribe, in one of these formats: flac, mp3, mp4, mpeg, " +
                    "mpga, m4a, ogg, wav, or webm.")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(LLMUtils.getEnumOptions(
                        Arrays.stream(OpenAiAudioApi.WhisperModel.values())
                            .collect(Collectors.toMap(
                                OpenAiAudioApi.WhisperModel::getValue, OpenAiAudioApi.WhisperModel::getValue)))),
            LANGUAGE_PROPERTY,
            string(PROMPT)
                .label("Prompt")
                .description(
                    "An optional text to guide the model's style or continue a previous audio segment. The prompt " +
                        "should match the audio language.")
                .required(false),
            object(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format of the transcript output")
                .options(LLMUtils.getEnumOptions(
                    Arrays.stream(OpenAiAudioApi.TranscriptResponseFormat.values())
                        .collect(Collectors.toMap(
                            OpenAiAudioApi.TranscriptResponseFormat::getValue, clas -> clas))))
                .required(true),
            number(TEMPERATURE)
                .label("Temperature")
                .description(
                    "The sampling temperature, between 0 and 1. Higher values like will make the output more random, " +
                        "while lower values will make it more focused and deterministic. ")
                .defaultValue(0)
                .minValue(0)
                .maxValue(1)
                .required(false))
        .outputSchema(string())
        .perform(OpenAICreateTranscriptionAction::perform);

    private OpenAICreateTranscriptionAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MalformedURLException {

        AudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
            .withModel(inputParameters.getRequiredString(MODEL))
            .withPrompt(inputParameters.getString(PROMPT))
            .withLanguage(inputParameters.getString(LANGUAGE))
            .withResponseFormat(inputParameters.get(RESPONSE_FORMAT, OpenAiAudioApi.TranscriptResponseFormat.class))
            .withTemperature(inputParameters.getFloat(TEMPERATURE))
            .build();
        OpenAiAudioTranscriptionModel transcriptionModel =
            new OpenAiAudioTranscriptionModel(new OpenAiAudioApi(connectionParameters.getString(TOKEN)),
                (OpenAiAudioTranscriptionOptions) transcriptionOptions);

        FileEntry fileEntry = inputParameters.getFileEntry(FILE);
        AudioTranscriptionPrompt audio = new AudioTranscriptionPrompt(new UrlResource(fileEntry.getUrl()));
        AudioTranscriptionResponse response = transcriptionModel.call(audio);
        return response.getResult()
            .getOutput();
    }
}
