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

package com.bytechef.platform.configuration.workflow.diff;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SensitiveValueMaskerTest {

    private static final String MASKED_VALUE_PATTERN = "^" + SensitiveValueMasker.MASK + ":[0-9a-f]{16}$";

    @Test
    void testMasksSensitiveKeysCaseAndSeparatorInsensitive() {
        Map<String, Object> masked = SensitiveValueMasker.mask(
            Map.of("Api_Key", "a", "client-secret", "b", "AUTHORIZATION", "c"));

        assertThat((String) masked.get("Api_Key")).startsWith(SensitiveValueMasker.MASK);
        assertThat((String) masked.get("client-secret")).startsWith(SensitiveValueMasker.MASK);
        assertThat((String) masked.get("AUTHORIZATION")).startsWith(SensitiveValueMasker.MASK);
    }

    @Test
    void testKeepsInsensitiveKeys() {
        Map<String, Object> masked = SensitiveValueMasker.mask(Map.of("url", "u", "username", "n"));

        assertThat(masked).containsEntry("url", "u")
            .containsEntry("username", "n");
    }

    @Test
    void testMasksNestedValues() {
        Map<String, Object> masked = SensitiveValueMasker.mask(
            Map.of("parameters", Map.of("headers", Map.of("Authorization", "Bearer x"))));

        Map<?, ?> parameters = (Map<?, ?>) masked.get("parameters");
        Map<?, ?> headers = (Map<?, ?>) parameters.get("headers");

        assertThat((String) headers.get("Authorization")).startsWith(SensitiveValueMasker.MASK);
    }

    @Test
    void testKeepsExpressionValues() {
        Map<String, Object> masked = SensitiveValueMasker.mask(Map.of("token", "${trigger.token}"));

        assertThat(masked).containsEntry("token", "${trigger.token}");
    }

    @Test
    void testMaskPreservesInequalityOfDifferentSecrets() {
        Map<String, Object> first = SensitiveValueMasker.mask(Map.of("password", "one"));
        Map<String, Object> second = SensitiveValueMasker.mask(Map.of("password", "two"));
        Map<String, Object> repeated = SensitiveValueMasker.mask(Map.of("password", "one"));

        assertThat(first.get("password")).isNotEqualTo(second.get("password"));
        assertThat(first.get("password")).isEqualTo(repeated.get("password"));
    }

    @Test
    void testMasksKeysEndingWithSensitiveSuffix() {
        Map<String, Object> masked = SensitiveValueMasker.mask(
            Map.ofEntries(
                Map.entry("X-API-Key", "a"), Map.entry("X-Auth-Token", "b"), Map.entry("Proxy-Authorization", "c"),
                Map.entry("webhookSecret", "d"), Map.entry("secretKey", "e"), Map.entry("secretAccessKey", "f"),
                Map.entry("bearerToken", "g"), Map.entry("authToken", "h"), Map.entry("db_passwd", "i"),
                Map.entry("awsCredential", "j"), Map.entry("sshPassphrase", "k")));

        assertThat(masked).hasSize(11);

        for (Map.Entry<String, Object> entry : masked.entrySet()) {
            assertThat(entry.getValue()).as(entry.getKey())
                .asString()
                .matches(MASKED_VALUE_PATTERN);
        }
    }

    @Test
    void testKeepsKeysThatOnlyContainSensitiveWords() {
        Map<String, Object> masked = SensitiveValueMasker.mask(
            Map.of("maxTokens", 100, "tokenUrl", "https://example.com/token", "tokenType", "Bearer"));

        assertThat(masked).containsExactlyInAnyOrderEntriesOf(
            Map.of("maxTokens", 100, "tokenUrl", "https://example.com/token", "tokenType", "Bearer"));
    }

    @Test
    void testMasksListAndMapValuesOfSensitiveKeysAsAWhole() {
        Map<String, Object> masked = SensitiveValueMasker.mask(
            Map.of("apiKey", List.of("a", "b"), "clientCredentials", Map.of("id", "x", "secret", "y")));

        assertThat(masked).containsOnlyKeys("apiKey", "clientCredentials");
        assertThat(masked.get("apiKey")).asString()
            .matches(MASKED_VALUE_PATTERN);
        assertThat(masked.get("clientCredentials")).asString()
            .matches(MASKED_VALUE_PATTERN);
    }
}
