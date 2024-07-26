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
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.FILE;
import static com.bytechef.component.openai.constant.OpenAIConstants.LANGUAGE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;

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
                .options(
                    option(OpenAiAudioApi.WhisperModel.WHISPER_1.value, OpenAiAudioApi.WhisperModel.WHISPER_1.value)),
            string(LANGUAGE)
                .label("Language")
                .description("The language of the input audio.")
                .options(
                    List.of(
                        option("Afrikaans", "af"),
                        option("Arabic", "ar"),
                        option("Armenian", "hy"),
                        option("Azerbaijani", "az"),
                        option("Belarusian", "be"),
                        option("Bosnian", "bs"),
                        option("Bulgarian", "bg"),
                        option("Catalan", "ca"),
                        option("Chinese (Simplified)", "zh"),
                        option("Croatian", "hr"),
                        option("Czech", "cs"),
                        option("Danish", "da"),
                        option("Dutch", "nl"),
                        option("Greek", "el"),
                        option("Estonian", "et"),
                        option("English", "en"),
                        option("Finnish", "fi"),
                        option("French", "fr"),
                        option("Galician", "gl"),
                        option("German", "de"),
                        option("Hebrew", "he"),
                        option("Hindi", "hi"),
                        option("Hungarian", "hu"),
                        option("Icelandic", "is"),
                        option("Indonesian", "id"),
                        option("Italian", "it"),
                        option("Japanese", "ja"),
                        option("Kazakh", "kk"),
                        option("Kannada", "kn"),
                        option("Korean", "ko"),
                        option("Lithuanian", "lt"),
                        option("Latvian", "lv"),
                        option("Maori", "ma"),
                        option("Macedonian", "mk"),
                        option("Marathi", "mr"),
                        option("Malay", "ms"),
                        option("Nepali", "ne"),
                        option("Norwegian", "no"),
                        option("Persian", "fa"),
                        option("Polish", "pl"),
                        option("Portuguese", "pt"),
                        option("Romanian", "ro"),
                        option("Russian", "ru"),
                        option("Slovak", "sk"),
                        option("Slovenian", "sl"),
                        option("Serbian", "sr"),
                        option("Spanish", "es"),
                        option("Swedish", "sv"),
                        option("Swahili", "sw"),
                        option("Tamil", "ta"),
                        option("Tagalog", "tl"),
                        option("Thai", "th"),
                        option("Turkish", "tr"),
                        option("Ukrainian", "uk"),
                        option("Urdu", "ur"),
                        option("Vietnamese", "vi"),
                        option("Welsh", "cy")))
                .required(false),
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
