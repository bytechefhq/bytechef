/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.stt.deepgram;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.stt.SttProvider;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.stt", name = "provider", havingValue = "deepgram")
public class DeepgramSttProvider implements SttProvider {

    public static final String KEY = "deepgram";

    private final RestClient restClient;

    private final String model;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DeepgramSttProvider(RestClient deepgramSttRestClient, ApplicationProperties applicationProperties) {
        this.restClient = deepgramSttRestClient;
        this.model = applicationProperties.getAi()
            .getProvider()
            .getStt()
            .getDeepgram()
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

        String uri = UriComponentsBuilder.fromPath("/v1/listen")
            .queryParam("model", model)
            .queryParam("language", request.locale() == null ? "en" : request.locale())
            .queryParam("smart_format", "true")
            .build()
            .toUriString();

        DeepgramResponse response = restClient.post()
            .uri(uri)
            .header("Authorization", "Token " + apiKey)
            .contentType(MediaType.parseMediaType(request.mimeType()))
            .body(new InputStreamResource(request.audio()))
            .retrieve()
            .body(DeepgramResponse.class);

        if (response == null || response.results == null || response.results.channels == null
            || response.results.channels.isEmpty()
            || response.results.channels.get(0).alternatives == null
            || response.results.channels.get(0).alternatives.isEmpty()) {

            throw new IllegalStateException("Empty response from Deepgram STT");
        }

        String text = response.results.channels.get(0).alternatives.get(0).transcript;
        long durationMs = response.metadata == null || response.metadata.duration == null
            ? 0L : Math.round(response.metadata.duration * 1000);

        return new TranscriptResult(text == null ? "" : text, durationMs, request.locale());
    }

    @SuppressWarnings("PMD")
    @SuppressFBWarnings("UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD")
    private static final class DeepgramResponse {
        public Results results;
        public Metadata metadata;

        static final class Results {
            public List<Channel> channels;
        }

        static final class Channel {
            public List<Alternative> alternatives;
        }

        static final class Alternative {
            public String transcript;
        }

        static final class Metadata {
            public Double duration;
        }
    }
}
