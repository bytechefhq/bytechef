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

package com.bytechef.component.ai.llm.router.nano.gpt.action;

import static com.bytechef.component.ai.llm.constant.LLMConstants.CREATE_SPEECH;
import static com.bytechef.component.ai.llm.constant.LLMConstants.INPUT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.MODEL;
import static com.bytechef.component.ai.llm.constant.LLMConstants.RESPONSE_FORMAT;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SPEED;
import static com.bytechef.component.ai.llm.constant.LLMConstants.VOICE;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.BASE_URL;
import static com.bytechef.component.ai.llm.router.nano.gpt.constant.NanoGptConstants.SPEECH_MODEL_PROPERTY;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * @author Marko Kriskovic
 */
public class NanoGptCreateSpeechAction {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_POLL_ATTEMPTS = 40;
    private static final int POLL_INTERVAL_MS = 3000;

    private record AudioFetchResult(byte[] bytes, String extension, String audioUrl) {
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_SPEECH)
        .title("Create Speech")
        .description("Generate an audio file from the input text.")
        .help("", "https://docs.bytechef.io/reference/components/nano-gpt_v1#create-speech")
        .properties(
            SPEECH_MODEL_PROPERTY,
            string(INPUT)
                .label("Input")
                .description("The text to synthesize.")
                .required(true),
            string(VOICE)
                .label("Voice")
                .description("Voice identifier (model-specific).")
                .required(false),
            string(RESPONSE_FORMAT)
                .label("Response Format")
                .description("Audio output format (OpenAI models only).")
                .options(
                    option("MP3", "mp3"),
                    option("WAV", "wav"),
                    option("OPUS", "opus"),
                    option("AAC", "aac"),
                    option("FLAC", "flac"),
                    option("PCM", "pcm"))
                .defaultValue("mp3")
                .required(false),
            number(SPEED)
                .label("Speed")
                .description("Playback speed (0.1–5). Not supported for gpt-4o-mini-tts.")
                .defaultValue(1.0)
                .minValue(0.1)
                .maxValue(5)
                .required(false))
        .output(outputSchema(object().properties(
            fileEntry("file").description("The generated audio file."),
            string("audioUrl").description("URL to the generated audio file"))))
        .perform(NanoGptCreateSpeechAction::perform);

    private NanoGptCreateSpeechAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("text", inputParameters.getRequiredString(INPUT));
        requestBody.put("model", inputParameters.getRequiredString(MODEL));

        String voice = inputParameters.getString(VOICE);

        if (voice != null && !voice.isBlank()) {
            requestBody.put("voice", voice);
        }

        String responseFormat = inputParameters.getString(RESPONSE_FORMAT, "mp3");

        requestBody.put("response_format", responseFormat);

        Double speed = inputParameters.getDouble(SPEED);

        if (speed != null) {
            requestBody.put("speed", speed);
        }

        RestClient restClient = ModelUtils.getRestClientBuilder()
            .defaultHeader("x-api-key", connectionParameters.getRequiredString(TOKEN))
            .build();

        AudioFetchResult result = fetchAudio(requestBody, restClient, responseFormat);

        FileEntry fileEntry = context.file(
            file -> file.storeContent("speech." + result.extension(), new ByteArrayInputStream(result.bytes())));

        Map<String, Object> output = new HashMap<>();

        output.put("file", fileEntry);
        output.put("audioUrl", result.audioUrl());

        return output;
    }

    private static AudioFetchResult fetchAudio(
        Map<String, Object> requestBody, RestClient restClient, String defaultExtension) {

        AtomicReference<MediaType> contentTypeRef = new AtomicReference<>();

        byte[] initialBytes = restClient.post()
            .uri(BASE_URL + "/tts")
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .exchange((request, response) -> {
                contentTypeRef.set(response.getHeaders()
                    .getContentType());

                return response.getBody()
                    .readAllBytes();
            });

        MediaType contentType = contentTypeRef.get();

        if (contentType == null || !contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return new AudioFetchResult(initialBytes, defaultExtension, null);
        }

        try {
            Map<String, Object> json = OBJECT_MAPPER.readValue(initialBytes, new TypeReference<>() {});

            if ("pending".equals(json.get("status"))) {
                json = pollForCompletion(json, restClient);
            }

            String audioContentType = (String) json.get("contentType");
            String extension = audioContentType != null
                ? mimeToExtension(audioContentType, defaultExtension)
                : defaultExtension;

            String audioUrl = (String) json.get("audioUrl");

            if (audioUrl != null) {
                byte[] audioBytes = restClient.get()
                    .uri(audioUrl)
                    .retrieve()
                    .body(byte[].class);

                return new AudioFetchResult(audioBytes, extension, audioUrl);
            }
        } catch (IOException ignored) {
            return new AudioFetchResult(initialBytes, defaultExtension, null);
        }

        return new AudioFetchResult(initialBytes, defaultExtension, null);
    }

    private static Map<String, Object> pollForCompletion(Map<String, Object> ticket, RestClient restClient) {
        String runId = (String) ticket.get("runId");
        String model = (String) ticket.get("model");
        Number cost = (Number) ticket.get("cost");
        String paymentSource = (String) ticket.get("paymentSource");

        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread()
                    .interrupt();

                throw new RuntimeException("TTS polling interrupted", interruptedException);
            }

            String statusUrl = BASE_URL + "/tts/status" + "?runId=" + runId + "&model=" + model + "&isApiRequest=true";

            if (cost != null) {
                statusUrl += "&cost=" + cost;
            }

            if (paymentSource != null) {
                statusUrl += "&paymentSource=" + paymentSource;
            }

            byte[] statusBytes = restClient.get()
                .uri(statusUrl)
                .retrieve()
                .body(byte[].class);

            Map<String, Object> statusResponse;

            try {
                statusResponse = OBJECT_MAPPER.readValue(statusBytes, new TypeReference<>() {});
            } catch (IOException parseException) {
                throw new RuntimeException("Failed to parse TTS status response", parseException);
            }

            String status = (String) statusResponse.get("status");

            if ("completed".equals(status)) {
                return statusResponse;
            }

            if ("failed".equals(status)) {
                throw new RuntimeException("TTS generation failed: " + statusResponse.get("error"));
            }
        }

        throw new RuntimeException("TTS generation timed out after " + MAX_POLL_ATTEMPTS + " polling attempts");
    }

    private static String mimeToExtension(String mimeType, String fallback) {
        return switch (mimeType) {
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/wav", "audio/wave", "audio/x-wav" -> "wav";
            case "audio/ogg" -> "ogg";
            case "audio/flac" -> "flac";
            case "audio/aac" -> "aac";
            case "audio/opus" -> "opus";
            default -> fallback;
        };
    }
}
