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

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.stt.SttProvider;
import com.bytechef.platform.ai.stt.SttProvider.TranscribeRequest;
import com.bytechef.platform.ai.stt.SttProvider.TranscriptResult;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Selects the configured {@link SttProvider} (only the active provider bean is present, gated by
 * {@code bytechef.ai.stt.provider}) and delegates transcription to it, supplying the provider's API key.
 *
 * @author Ivica Cardic
 */
@Service
public class TranscribeServiceImpl implements TranscribeService {

    private final ApplicationProperties applicationProperties;
    private final List<SttProvider> sttProviders;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public TranscribeServiceImpl(ApplicationProperties applicationProperties, List<SttProvider> sttProviders) {
        this.applicationProperties = applicationProperties;
        this.sttProviders = sttProviders;
    }

    @Override
    public TranscriptResult transcribe(InputStream audio, @Nullable String mimeType, @Nullable String locale) {
        ApplicationProperties.Ai.Stt.Provider provider = applicationProperties.getAi()
            .getStt()
            .getProvider();

        SttProvider sttProvider = sttProviders.stream()
            .filter(candidate -> candidate.getKey()
                .equalsIgnoreCase(provider.name()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No speech-to-text provider available for " + provider));

        return sttProvider.transcribe(
            new TranscribeRequest(audio, mimeType, locale, Map.of("apiKey", resolveApiKey(provider))));
    }

    /**
     * Resolves the API key for the active provider. OpenAI (the default provider) uses the shared OpenAI provider key;
     * Deepgram/ElevenLabs read theirs from the provider's own configuration and therefore need no key supplied here.
     */
    private String resolveApiKey(ApplicationProperties.Ai.Stt.Provider provider) {
        if (provider == ApplicationProperties.Ai.Stt.Provider.OPENAI) {
            String apiKey = applicationProperties.getAi()
                .getProvider()
                .getOpenAi()
                .getApiKey();

            return apiKey == null ? "" : apiKey;
        }

        return "";
    }
}
