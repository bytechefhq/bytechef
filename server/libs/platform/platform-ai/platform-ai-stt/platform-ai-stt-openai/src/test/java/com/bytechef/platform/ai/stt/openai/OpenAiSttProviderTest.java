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

class OpenAiSttProviderTest {

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
    void testTranscribePostsMultipartAndParsesResponse() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("{\"text\":\"hello world\",\"language\":\"english\",\"duration\":1.234}"));

        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getAi()
            .getProvider()
            .getStt()
            .getOpenAi()
            .getOptions()
            .setModel("gpt-4o-mini-transcribe");

        OpenAiSttProvider provider = new OpenAiSttProvider(
            RestClient.builder()
                .baseUrl(mockWebServer.url("/")
                    .toString())
                .build(),
            applicationProperties);

        TranscriptResult result = provider.transcribe(new TranscribeRequest(
            new ByteArrayInputStream(new byte[] {
                1, 2, 3, 4
            }),
            "audio/webm", "en",
            Map.of("apiKey", "sk-test")));

        assertThat(result.text()).isEqualTo("hello world");
        assertThat(result.durationMs()).isEqualTo(1234L);
        assertThat(result.detectedLocale()).isEqualTo("english");

        RecordedRequest recorded = mockWebServer.takeRequest();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/v1/audio/transcriptions");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer sk-test");
        assertThat(recorded.getHeader("Content-Type")).startsWith("multipart/form-data");
    }

    @Test
    void testGetKeyReturnsExpected() {
        OpenAiSttProvider provider = new OpenAiSttProvider(
            RestClient.builder()
                .build(),
            new ApplicationProperties());

        assertThat(provider.getKey()).isEqualTo("openai");
    }
}
