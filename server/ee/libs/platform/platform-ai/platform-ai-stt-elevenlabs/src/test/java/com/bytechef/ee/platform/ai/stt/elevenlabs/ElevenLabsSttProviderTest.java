/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.stt.elevenlabs;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.ByteArrayInputStream;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * @version ee
 */
class ElevenLabsSttProviderTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();

        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void testTranscribePostsMultipartAndParses() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"text\":\"voice text\",\"language_code\":\"en\"}"));

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getStt()
            .getElevenlabs()
            .getOptions()
            .setModel("scribe_v1");

        ElevenLabsSttProvider provider = new ElevenLabsSttProvider(
            RestClient.builder()
                .baseUrl(mockWebServer.url("/")
                    .toString())
                .build(),
            applicationProperties);

        TranscriptResult result = provider.transcribe(new TranscribeRequest(
            new ByteArrayInputStream(new byte[] {
                1, 2
            }),
            "audio/webm", "en",
            Map.of("apiKey", "el-test")));

        assertThat(result.text()).isEqualTo("voice text");
        assertThat(result.detectedLocale()).isEqualTo("en");

        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/speech-to-text");
        assertThat(recorded.getHeader("xi-api-key")).isEqualTo("el-test");
    }

    @Test
    void testGetKey() {
        assertThat(new ElevenLabsSttProvider(RestClient.builder()
            .build(), new ApplicationProperties()).getKey())
                .isEqualTo("elevenlabs");
    }
}
