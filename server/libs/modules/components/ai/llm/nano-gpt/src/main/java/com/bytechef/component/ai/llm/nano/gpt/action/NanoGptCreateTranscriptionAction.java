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

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_TRANSCRIPTION;
import static com.bytechef.component.ai.llm.constant.LLMConstants.FILE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.LANGUAGE;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TRANSCRIBE_URL;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TRANSCRIPTION_LANGUAGE_PROPERTY;
import static com.bytechef.component.ai.llm.nano.gpt.constant.NanoGptConstants.TRANSCRIPTION_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
public class NanoGptCreateTranscriptionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TRANSCRIPTION)
        .title("Create Transcription")
        .description("Transcribes audio into text.")
        .properties(
            TRANSCRIPTION_MODEL_PROPERTY,
            fileEntry(FILE)
                .label("File")
                .description(
                    "The audio file to transcribe. Supported formats: MP3, WAV, M4A, OGG, AAC (max 3MB).")
                .required(true),
            TRANSCRIPTION_LANGUAGE_PROPERTY)
        .output(outputSchema(string()))
        .perform(NanoGptCreateTranscriptionAction::perform);

    private NanoGptCreateTranscriptionAction() {
    }

    @SuppressWarnings("unchecked")
    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        FileEntry fileEntry = inputParameters.getFileEntry(FILE);
        byte[] audioBytes = context.file(file -> file.readAllBytes(fileEntry));
        String filename = fileEntry.getName() != null ? fileEntry.getName() : "audio";

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        formData.add("audio", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        formData.add("model", inputParameters.getRequiredString(MODEL));

        String language = inputParameters.getString(LANGUAGE);

        if (language != null) {
            formData.add("language", language);
        }

        RestClient restClient = ModelUtils.getRestClientBuilder()
            .defaultHeader("x-api-key", connectionParameters.getRequiredString(TOKEN))
            .build();

        Map<String, Object> response = restClient.post()
            .uri(TRANSCRIBE_URL)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(formData)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return (String) response.get("transcription");
    }
}
