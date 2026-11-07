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

package com.bytechef.platform.ai.stt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.stt.SttProvider;
import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class TranscribeServiceTest {

    @Test
    void testTranscribeSelectsConfiguredProviderAndSuppliesApiKey() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        when(applicationProperties.getAi()
            .getStt()
            .getProvider()).thenReturn(ApplicationProperties.Ai.Stt.Provider.OPENAI);
        when(applicationProperties.getAi()
            .getProvider()
            .getOpenAi()
            .getApiKey()).thenReturn("sk-test");

        AtomicReference<TranscribeRequest> captured = new AtomicReference<>();

        SttProvider openAiProvider = new SttProvider() {

            @Override
            public String getKey() {
                return "openai";
            }

            @Override
            public TranscriptResult transcribe(TranscribeRequest request) {
                captured.set(request);

                return new TranscriptResult("hello world", 1200, "en");
            }
        };

        TranscribeServiceImpl transcribeService = new TranscribeServiceImpl(
            applicationProperties, List.of(openAiProvider));

        TranscriptResult result = transcribeService.transcribe(
            new ByteArrayInputStream(new byte[] {
                1, 2, 3
            }), "audio/webm", "en");

        assertThat(result.text()).isEqualTo("hello world");
        assertThat(captured.get()
            .connectionParameters()).containsEntry("apiKey", "sk-test");
        assertThat(captured.get()
            .mimeType()).isEqualTo("audio/webm");
    }

    @Test
    void testTranscribeThrowsWhenConfiguredProviderIsUnavailable() {
        ApplicationProperties applicationProperties = mock(ApplicationProperties.class, RETURNS_DEEP_STUBS);

        when(applicationProperties.getAi()
            .getStt()
            .getProvider()).thenReturn(ApplicationProperties.Ai.Stt.Provider.DEEPGRAM);

        TranscribeServiceImpl transcribeService = new TranscribeServiceImpl(applicationProperties, List.of());

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(
            () -> transcribeService.transcribe(new ByteArrayInputStream(new byte[0]), "audio/webm", null));
    }
}
