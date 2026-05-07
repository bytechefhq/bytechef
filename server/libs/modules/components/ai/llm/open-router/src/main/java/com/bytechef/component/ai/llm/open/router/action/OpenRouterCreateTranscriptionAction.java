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

package com.bytechef.component.ai.llm.open.router.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.BASE_URL;
import static com.bytechef.component.ai.llm.open.router.constant.OpenRouterConstants.TRANSCRIPTION_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
public class OpenRouterCreateTranscriptionAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TRANSCRIPTION)
        .title("Create Transcription")
        .description("Transcribes audio into text.")
        .properties(
            TRANSCRIPTION_MODEL_PROPERTY,
            fileEntry(FILE)
                .label("File")
                .description(
                    "The audio file to transcribe. Supported formats: wav, mp3, flac, m4a, ogg, webm, aac.")
                .required(true),
            LANGUAGE_PROPERTY,
            number(TEMPERATURE)
                .label("Temperature")
                .description("Sampling temperature for transcription.")
                .defaultValue(0.5)
                .minValue(0)
                .maxValue(1)
                .required(false))
        .output(outputSchema(string()))
        .perform(OpenRouterCreateTranscriptionAction::perform);

    private OpenRouterCreateTranscriptionAction() {
    }

    @SuppressWarnings("unchecked")
    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        FileEntry fileEntry = inputParameters.getFileEntry(FILE);

        byte[] audioBytes = context.file(file -> file.readAllBytes(fileEntry));
        String base64Data = Base64.getEncoder()
            .encodeToString(audioBytes);
        String format = fileEntry.getExtension() != null ? fileEntry.getExtension() : "wav";

        Map<String, Object> inputAudio = new HashMap<>();

        inputAudio.put("data", base64Data);
        inputAudio.put("format", format);

        Map<String, Object> body = new HashMap<>();

        body.put("input_audio", inputAudio);
        body.put("model", inputParameters.getRequiredString(MODEL));

        String language = inputParameters.getString(LANGUAGE);

        if (language != null) {
            body.put("language", language);
        }

        Double temperature = inputParameters.getDouble(TEMPERATURE);

        if (temperature != null) {
            body.put("temperature", temperature);
        }

        RestClient restClient = ModelUtils.getRestClientBuilder()
            .baseUrl(BASE_URL)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + connectionParameters.getRequiredString(TOKEN))
            .build();

        Map<String, Object> response = restClient.post()
            .uri("/audio/transcriptions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return (String) response.get("text");
    }
}
