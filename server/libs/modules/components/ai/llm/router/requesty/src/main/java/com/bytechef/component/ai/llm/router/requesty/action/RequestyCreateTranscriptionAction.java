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

package com.bytechef.component.ai.llm.router.requesty.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE_PROPERTY;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.TEMPERATURE;
import static com.bytechef.component.ai.llm.router.requesty.constant.RequestyConstants.BASE_URL;
import static com.bytechef.component.ai.llm.router.requesty.constant.RequestyConstants.TRANSCRIPTION_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Thibault Jaigu
 * @author Igor Beslic
 */
public class RequestyCreateTranscriptionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TRANSCRIPTION)
        .title("Create Transcription")
        .description("Transcribes audio into text.")
        .help("", "https://docs.bytechef.io/reference/components/requesty_v1#create-transcription")
        .properties(
            TRANSCRIPTION_MODEL_PROPERTY,
            fileEntry(FILE)
                .label("File")
                .description(
                    "The audio file to transcribe. Supported formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, " +
                        "webm.")
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
        .perform(RequestyCreateTranscriptionAction::perform);

    private RequestyCreateTranscriptionAction() {
    }

    @SuppressWarnings("unchecked")
    public static String perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        FileEntry fileEntry = inputParameters.getFileEntry(FILE);
        byte[] audioBytes = context.file(file -> file.readAllBytes(fileEntry));
        String filename = fileEntry.getName() != null ? fileEntry.getName() : "audio.wav";

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        formData.add(FILE, new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        formData.add(MODEL, inputParameters.getRequiredString(MODEL));

        String language = inputParameters.getString(LANGUAGE);

        if (language != null) {
            formData.add(LANGUAGE, language);
        }

        Double temperature = inputParameters.getDouble(TEMPERATURE);

        if (temperature != null) {
            formData.add(TEMPERATURE, String.valueOf(temperature));
        }

        Context.Http.Response response1 = context.http(http -> http.post(BASE_URL + "/audio/transcriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + connectionParameters.getRequiredString(TOKEN))
            .body(Context.Http.Body.of(formData, Context.Http.BodyContentType.FORM_DATA)))
            .execute();

        Map<String, String> body = response1.getBody(new TypeReference<Map<String, String>>() {});

        return body.get("text");
    }
}
