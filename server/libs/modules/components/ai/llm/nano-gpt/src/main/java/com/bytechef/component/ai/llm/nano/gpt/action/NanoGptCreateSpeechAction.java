/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.ai.llm.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_SPEECH;
import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.BASE_URL;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.SPEECH_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
public class NanoGptCreateSpeechAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SPEECH)
        .title("Create Speech")
        .description("Generate an audio file from the input text")
        .properties(
            SPEECH_MODEL_PROPERTY,
            string(INPUT)
                .label("Input")
                .description("The text to synthesize.")
                .required(true),
            string(VOICE)
                .label("Voice")
                .description("Voice identifier (provider-specific).")
                .required(true),
            string(RESPONSE_FORMAT)
                .label("Response Format")
                .description("Audio output file format.")
                .options(
                    option("MP3", "mp3"),
                    option("PCM", "pcm"))
                .defaultValue("pcm")
                .required(false),
            number(SPEED)
                .label("Speed")
                .description("Playback speed multiplier. Only used by models that support it.")
                .defaultValue(1.0)
                .required(false))
        .output(outputSchema(fileEntry().description("The generated audio file.")))
        .perform(NanoGptCreateSpeechAction::perform);

    private NanoGptCreateSpeechAction() {
    }

    public static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String responseFormat = inputParameters.getString(RESPONSE_FORMAT, "pcm");

        Map<String, Object> body = new HashMap<>();

        body.put("input", inputParameters.getRequiredString(INPUT));
        body.put("model", inputParameters.getRequiredString(MODEL));
        body.put("response_format", responseFormat);
        body.put("voice", inputParameters.getRequiredString(VOICE));

        Double speed = inputParameters.getDouble(SPEED);

        if (speed != null) {
            body.put("speed", speed);
        }

        RestClient restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + connectionParameters.getRequiredString(TOKEN))
            .build();

        byte[] audioBytes = restClient.post()
            .uri("/audio/speech")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(byte[].class);

        return context.file(
            file -> file.storeContent("speech." + responseFormat, new ByteArrayInputStream(audioBytes)));
    }
}
