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

package com.bytechef.platform.ai.provider.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class AiProviderAuditMapperTest {

    private final AiProviderAuditMapper aiProviderAuditMapper = new AiProviderAuditMapper();

    @ParameterizedTest
    @CsvSource({
        "http://localhost:11434, http://localhost:11434",
        "https://user:secret@ollama.example.com:8443/api?token=secret#key, https://ollama.example.com:8443/api",
        "https://ollama.example.com/v1/?api_key=secret, https://ollama.example.com/v1/",
        "' http://ollama.internal ', http://ollama.internal"
    })
    void testUrlIsRecordedWithoutCredentialsQueryOrFragment(String url, String expectedUrl) {
        assertThat(aiProviderAuditMapper.map(createInvocation(url))).isEqualTo(Map.of("url", expectedUrl));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "not a url", "ollama.internal:11434", "/relative/path", "http://"
    })
    void testUnparsableUrlIsRecordedAsUnparseable(String url) {
        assertThat(aiProviderAuditMapper.map(createInvocation(url)))
            .containsExactly(Map.entry("url", AiProviderAuditMapper.UNPARSEABLE_URL));
    }

    @Test
    void testMissingUrlIsNotRecorded() {
        assertThat(aiProviderAuditMapper.map(createInvocation(null))).isEmpty();
        assertThat(aiProviderAuditMapper.map(createInvocation(" "))).isEmpty();
    }

    private static AuditInvocation createInvocation(@Nullable String url) {
        Map<String, @Nullable Object> arguments = new HashMap<>();

        arguments.put("url", url);

        return new AuditInvocation(
            AiProviderAuditEvents.AI_PROVIDER_UPDATED, arguments, null, null, AuditOutcome.SUCCESS, null);
    }
}
