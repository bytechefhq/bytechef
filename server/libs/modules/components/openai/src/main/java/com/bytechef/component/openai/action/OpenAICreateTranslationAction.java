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
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_TRANSLATION;
import static com.bytechef.component.openai.constant.OpenAIConstants.FILE;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.PROMPT;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.TEMPERATURE;
import static com.bytechef.component.openai.constant.OpenAIConstants.WHISPER_1;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.theokanning.openai.audio.CreateTranslationRequest;
import com.theokanning.openai.audio.TranslationResult;
import com.theokanning.openai.service.OpenAiService;
import java.io.File;

/**
 * @author Monika Domiter
 */
public class OpenAICreateTranslationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TRANSLATION)
        .title("Create translation")
        .description("Translates audio into English.")
        .properties(
            fileEntry(FILE)
                .label("File")
                .description(
                    "The audio file object translate, in one of these formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, " +
                        "wav, or webm.")
                .required(true),
            string(MODEL)
                .label("Model")
                .description("ID of the model to use.")
                .required(true)
                .options(option(WHISPER_1, WHISPER_1))
                .defaultValue(WHISPER_1),
            string(PROMPT)
                .label("Prompt")
                .description(
                    "An optional text to guide the model's style or continue a previous audio segment. The prompt " +
                        "should be in English.")
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
                        "while lower values will make it more focused and deterministic.")
                .defaultValue(0)
                .minValue(0)
                .maxValue(1)
                .required(false))
        .outputSchema(
            object()
                .properties(
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
                                        .items(
                                            integer("token")),
                                    number("temperature"),
                                    number("averageLogProb"),
                                    number("compressionRatio"),
                                    number("noSpeechProb"),
                                    bool("transientFlag")))))
        .perform(OpenAICreateTranslationAction::perform);

    private OpenAICreateTranslationAction() {
    }

    public static TranslationResult perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        OpenAiService openAiService = new OpenAiService((String) connectionParameters.get(TOKEN));

        CreateTranslationRequest createTranslationRequest = new CreateTranslationRequest();

        createTranslationRequest.setModel(inputParameters.getRequiredString(MODEL));
        createTranslationRequest.setPrompt(inputParameters.getString(PROMPT));
        createTranslationRequest.setResponseFormat(inputParameters.getString(RESPONSE_FORMAT));
        createTranslationRequest.setTemperature(inputParameters.getDouble(TEMPERATURE));

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE);

        return openAiService.createTranslation(
            createTranslationRequest, (File) context.file(file1 -> file1.toTempFile(fileEntry)));
    }
}
