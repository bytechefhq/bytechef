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

package com.bytechef.component.azure.openai.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.llm.constant.LLMConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.llm.constant.LLMConstants.ENDPOINT;
import static com.bytechef.component.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.llm.constant.LLMConstants.LANGUAGE_PROPERTY;
import static com.bytechef.component.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.llm.constant.LLMConstants.PROMPT;
import static com.bytechef.component.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.llm.constant.LLMConstants.TEMPERATURE;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.llm.Transcript;
import com.bytechef.component.llm.util.LLMUtils;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.ai.audio.transcription.AudioTranscriptionOptions;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionModel;
import org.springframework.ai.azure.openai.AzureOpenAiAudioTranscriptionOptions;
import org.springframework.ai.model.Model;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
public class AzureOpenAICreateTranscriptionAction {

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
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(AzureOpenAiAudioTranscriptionOptions.WhisperModel.values())
                            .collect(
                                Collectors.toMap(
                                    AzureOpenAiAudioTranscriptionOptions.WhisperModel::getValue,
                                    AzureOpenAiAudioTranscriptionOptions.WhisperModel::getValue, (f, s) -> f)))),
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
                .options(
                    LLMUtils.getEnumOptions(
                        Arrays.stream(AzureOpenAiAudioTranscriptionOptions.TranscriptResponseFormat.values())
                            .collect(
                                Collectors.toMap(
                                    clazz -> String.valueOf(clazz.getValue()),
                                    AzureOpenAiAudioTranscriptionOptions.TranscriptResponseFormat::getValue,
                                    (f, s) -> f))))
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
        .output()
        .perform(AzureOpenAICreateTranscriptionAction::perform);

    private AzureOpenAICreateTranscriptionAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context)
        throws MalformedURLException {

        return Transcript.getResponse(TRANSCRIPT, inputParameters, connectionParameters);
    }

    private static final Transcript TRANSCRIPT = new Transcript() {

        @Override
        public AudioTranscriptionOptions createTranscriptOptions(Parameters inputParameters) {
            return AzureOpenAiAudioTranscriptionOptions.builder()
                .withModel(inputParameters.getRequiredString(MODEL))
                .withPrompt(inputParameters.getString(PROMPT))
                .withLanguage(inputParameters.getString(LANGUAGE))
                .withResponseFormat(inputParameters.get(RESPONSE_FORMAT,
                    AzureOpenAiAudioTranscriptionOptions.TranscriptResponseFormat.class))
                .withTemperature(inputParameters.getFloat(TEMPERATURE))
                .build();
        }

        @Override
        public Model<AudioTranscriptionPrompt, AudioTranscriptionResponse> createTranscriptionModel(
            Parameters inputParameters, Parameters connectionParameters) {

            OpenAIClient openAIClient = new OpenAIClientBuilder()
                .credential(new KeyCredential(connectionParameters.getString(TOKEN)))
                .endpoint(connectionParameters.getString(ENDPOINT))
                .buildClient();

            return new AzureOpenAiAudioTranscriptionModel(
                openAIClient, (AzureOpenAiAudioTranscriptionOptions) createTranscriptOptions(inputParameters));
        }
    };
}
