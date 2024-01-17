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
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.openai.constant.OpenAIConstants.CREATE_SPEECH;
import static com.bytechef.component.openai.constant.OpenAIConstants.INPUT;
import static com.bytechef.component.openai.constant.OpenAIConstants.MODEL;
import static com.bytechef.component.openai.constant.OpenAIConstants.RESPONSE_FORMAT;
import static com.bytechef.component.openai.constant.OpenAIConstants.SPEED;
import static com.bytechef.component.openai.constant.OpenAIConstants.VOICE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.FileEntry;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.theokanning.openai.audio.CreateSpeechRequest;
import com.theokanning.openai.service.OpenAiService;
import okhttp3.ResponseBody;

/**
 * @author Monika Domiter
 */
public class OpenAICreateSpeechAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SPEECH)
        .title("Create speech")
        .description("Generate an audio recording from the input text")
        .properties(
            string(MODEL)
                .label("Model")
                .description("Text-to-Speech model which will generate the audio.")
                .required(true)
                .options(
                    option("tts-1", "tts-1", "Model optimized for speed."),
                    option("tts-1-hd", "tts-1-hd", "Model optimized for quality.")),
            string(INPUT)
                .label("Input")
                .description("The text to generate audio for.")
                .maxLength(4096)
                .required(true),
            string(VOICE)
                .label("Voice")
                .description("The voice to use when generating the audio.")
                .options(
                    option("alloy", "alloy"),
                    option("echo", "echo"),
                    option("fable", "fable"),
                    option("onyx", "onyx"),
                    option("nova", "nova"),
                    option("schimmer", "schimmer"))
                .required(true),
            string(RESPONSE_FORMAT)
                .label("Response format")
                .description("The format to audio in.")
                .options(
                    option("mp3", "mp3"),
                    option("opus", "opus"),
                    option("aac", "aac"),
                    option("flac", "flac"))
                .defaultValue("mp3")
                .required(false),
            number(SPEED)
                .label("Speed")
                .description("The speed of the generated audio.")
                .defaultValue(1.0)
                .minValue(0.25)
                .maxValue(4.0)
                .required(false))
        .outputSchema(fileEntry())
        .perform(OpenAICreateSpeechAction::perform);

    private OpenAICreateSpeechAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String token = (String) connectionParameters.get(TOKEN);

        OpenAiService openAiService = new OpenAiService(token);

        CreateSpeechRequest createSpeechRequest = new CreateSpeechRequest();

        createSpeechRequest.setModel(inputParameters.getRequiredString(MODEL));
        createSpeechRequest.setInput(inputParameters.getRequiredString(INPUT));
        createSpeechRequest.setVoice(inputParameters.getRequiredString(VOICE));
        createSpeechRequest.setResponseFormat(inputParameters.getString(RESPONSE_FORMAT));
        createSpeechRequest.setSpeed(inputParameters.getDouble(SPEED));

        try (ResponseBody speech = openAiService.createSpeech(createSpeechRequest)) {
            return context
                .file(file -> file.storeContent(
                    "file." + inputParameters.getString(RESPONSE_FORMAT), speech.byteStream()));
        }
    }
}
