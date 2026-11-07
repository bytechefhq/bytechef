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

package com.bytechef.platform.ai.stt.openai;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.stt.SttProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "bytechef.ai.stt", name = "provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiSttProvider implements SttProvider {

    public static final String KEY = "openai";

    private final RestClient restClient;

    private final String model;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public OpenAiSttProvider(RestClient openAiSttRestClient, ApplicationProperties applicationProperties) {
        this.restClient = openAiSttRestClient;
        this.model = applicationProperties.getAi()
            .getProvider()
            .getStt()
            .getOpenAi()
            .getOptions()
            .getModel();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public TranscriptResult transcribe(TranscribeRequest request) {
        String apiKey = (String) request.connectionParameters()
            .getOrDefault("apiKey", "");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new InputStreamResource(request.audio()) {
            @Override
            public String getFilename() {
                return "audio." + extension(request.mimeType());
            }

            @Override
            public long contentLength() throws IOException {
                return -1;
            }
        });
        body.add("model", model);

        if (request.locale() != null && !request.locale()
            .isBlank()) {
            body.add("language", request.locale());
        }

        OpenAiResponse response = restClient.post()
            .uri("/v1/audio/transcriptions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(OpenAiResponse.class);

        if (response == null) {
            throw new IllegalStateException("Empty response from OpenAI STT");
        }

        long durationMs = response.duration == null ? 0L : Math.round(response.duration * 1000);

        return new TranscriptResult(response.text, durationMs, response.language);
    }

    private static String extension(String mimeType) {
        return switch (mimeType) {
            case "audio/webm" -> "webm";
            case "audio/mp4" -> "mp4";
            case "audio/wav" -> "wav";
            case "audio/mpeg" -> "mp3";
            case "audio/ogg" -> "ogg";
            default -> "bin";
        };
    }

    @SuppressWarnings("PMD")
    private static final class OpenAiResponse {
        public String text;
        public String language;
        public Double duration;
    }
}
