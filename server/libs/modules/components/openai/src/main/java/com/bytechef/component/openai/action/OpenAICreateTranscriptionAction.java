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
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.openai.constant.OpenAIConstants.FILE;
import static com.bytechef.component.openai.constant.OpenAIConstants.LANGUAGE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL_PROPERTY;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.WHISPER_1;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.audio.TranscriptionResult;
import com.theokanning.openai.service.OpenAiService;
import java.io.File;
import java.util.List;

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
            MODEL_PROPERTY
                .options(option(WHISPER_1, WHISPER_1))
                .defaultValue(WHISPER_1),
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
            string(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format of the transcript output")
                .options(
                    option("json", "json"),
                    option("text", "text"),
                    option("srt", "srt"),
                    option("verbose_json", "verbose_json"),
                    option("vtt", "vtt"))
                .defaultValue("json")
                .required(false),
            number(TEMPERATURE)
                .label("Temperature")
                .description(
                    "The sampling temperature, between 0 and 1. Higher values like will make the output more random, " +
                        "while lower values will make it more focused and deterministic. ")
                .defaultValue(0)
                .minValue(0)
                .maxValue(1)
                .required(false))
        .outputSchema(
            object().properties(
                string("text"),
                string("task"),
                string("language"),
                number("duration"),
                array("segments")
                    .items(
                        object()
                            .properties(
                                integer("id"),
                                integer("seek"),
                                number("start"),
                                number("end"),
                                string("text"),
                                array("tokens")
                                    .items(integer()),
                                number("temperature"),
                                number("averageLogProb"),
                                number("compressionRatio"),
                                number("noSpeechProb"),
                                bool("transientFlag")))))
        .perform(OpenAICreateTranscriptionAction::perform);

    private OpenAICreateTranscriptionAction() {
    }

    public static TranscriptionResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        OpenAiService openAiService = new OpenAiService((String) connectionParameters.get(TOKEN));

        CreateTranscriptionRequest createTranscriptionRequest = new CreateTranscriptionRequest();

        createTranscriptionRequest.setModel(inputParameters.getRequiredString(MODEL));
        createTranscriptionRequest.setLanguage(inputParameters.getString(LANGUAGE));
        createTranscriptionRequest.setPrompt(inputParameters.getString(PROMPT));
        createTranscriptionRequest.setResponseFormat(inputParameters.getString(RESPONSE_FORMAT));
        createTranscriptionRequest.setTemperature(inputParameters.getDouble(TEMPERATURE));

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        return openAiService.createTranscription(
            createTranscriptionRequest, (File) context.file(file -> file.toTempFile(fileEntry)));
    }
}
