/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.stt.deepgram;

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
 *
 * @author Ivica Cardic
 */
class DeepgramSttProviderTest {

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
    void testTranscribePostsBinaryAndParses() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("""
                {"results":{"channels":[{"alternatives":[{"transcript":"deep voice"}]}]},
                 "metadata":{"duration":2.5}}"""));

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getStt()
            .getDeepgram()
            .getOptions()
            .setModel("nova-3");

        DeepgramSttProvider provider = new DeepgramSttProvider(
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
            Map.of("apiKey", "dg-test")));

        assertThat(result.text()).isEqualTo("deep voice");
        assertThat(result.durationMs()).isEqualTo(2500L);

        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).contains("/v1/listen");
        assertThat(recorded.getPath()).contains("model=nova-3");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Token dg-test");
    }

    @Test
    void testGetKey() {
        assertThat(new DeepgramSttProvider(RestClient.builder()
            .build(), new ApplicationProperties()).getKey())
                .isEqualTo("deepgram");
    }
}
