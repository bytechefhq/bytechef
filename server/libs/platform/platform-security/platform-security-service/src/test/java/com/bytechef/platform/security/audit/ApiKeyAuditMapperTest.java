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

package com.bytechef.platform.security.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApiKeyAuditMapperTest {

    private final ApiKeyAuditMapper apiKeyAuditMapper = new ApiKeyAuditMapper();

    @Test
    void testCreatedRecordsIdNameAndTypeWithoutSecretKey() {
        ApiKey apiKey = createApiKey();
        String secretKey = String.valueOf(UUID.randomUUID());

        apiKey.setId(7L);
        apiKey.setSecretKey(secretKey);

        Map<String, String> data = apiKeyAuditMapper.map(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_CREATED, Map.of("apiKey", apiKey), apiKey, null, AuditOutcome.SUCCESS, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("apiKeyId", "7", "name", "automation key", "type", "AUTOMATION"));
        assertThat(data.values()).doesNotContain(secretKey);
    }

    @Test
    void testFailedCreateRecordsTheRequestedNameAndType() {
        Map<String, String> data = apiKeyAuditMapper.map(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_CREATED, Map.of("apiKey", createApiKey()), null, null, AuditOutcome.ERROR,
            IllegalArgumentException.class));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(Map.of("name", "automation key", "type", "AUTOMATION"));
    }

    @Test
    void testDeletedRecordsId() {
        Map<String, String> data = apiKeyAuditMapper.map(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_DELETED, Map.of("id", 7L), null, null, AuditOutcome.SUCCESS, null));

        assertThat(data).containsExactly(Map.entry("apiKeyId", "7"));
    }

    @Test
    void testRenamedArgumentFailsMappingInsteadOfRecordingNull() {
        AuditInvocation auditInvocation = new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_DELETED, Map.of("apiKeyId", 7L), null, null, AuditOutcome.SUCCESS, null);

        assertThatThrownBy(() -> apiKeyAuditMapper.map(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id");
    }

    private static ApiKey createApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("automation key");
        apiKey.setType(PlatformType.AUTOMATION);

        return apiKey;
    }
}
