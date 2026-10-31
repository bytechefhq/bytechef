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
import com.bytechef.platform.security.config.PlatformSecurityIntTestConfiguration;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformSecurityIntTestConfiguration.class)
class ApiKeyAuditMapperIntTest {

    @Autowired
    private ApiKeyAuditMapper apiKeyAuditMapper;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void beforeEach() {
        user = userRepository.save(newUser());
    }

    @AfterEach
    void afterEach() {
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
    }

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

    @Test
    void testCreatedPlatformIsTheSavedApiKeyType() {
        ApiKey apiKey = createApiKey();

        apiKey.setType(PlatformType.EMBEDDED);

        assertThat(apiKeyAuditMapper.platform(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_CREATED, Map.of("apiKey", createApiKey()), apiKey, null, AuditOutcome.SUCCESS,
            null)))
                .isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testFailedCreatePlatformIsTheRequestedApiKeyType() {
        assertThat(apiKeyAuditMapper.platform(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_CREATED, Map.of("apiKey", createApiKey()), null, null, AuditOutcome.ERROR,
            IllegalArgumentException.class)))
                .isEqualTo(PlatformType.AUTOMATION);
    }

    @Test
    void testDeleteCapturesTheStoredApiKeyType() {
        ApiKey apiKey = createApiKey();

        apiKey.setSecretKey(String.valueOf(UUID.randomUUID()));
        apiKey.setType(PlatformType.EMBEDDED);
        apiKey.setUserId(user.getId());

        ApiKey savedApiKey = apiKeyRepository.save(apiKey);

        Map<String, Object> arguments = Map.of("id", savedApiKey.getId());

        Object captured = apiKeyAuditMapper.capture(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_DELETED, arguments, null, null, AuditOutcome.SUCCESS, null));

        apiKeyRepository.deleteById(savedApiKey.getId());

        assertThat(captured).isEqualTo(PlatformType.EMBEDDED);
        assertThat(apiKeyAuditMapper.platform(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_DELETED, arguments, null, captured, AuditOutcome.SUCCESS, null)))
                .isEqualTo(PlatformType.EMBEDDED);
    }

    @Test
    void testDeleteOfAnUnknownApiKeyHasNoPlatform() {
        AuditInvocation auditInvocation = new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_DELETED, Map.of("id", Long.MAX_VALUE), null, null, AuditOutcome.SUCCESS, null);

        assertThat(apiKeyAuditMapper.capture(auditInvocation)).isNull();
        assertThat(apiKeyAuditMapper.platform(auditInvocation)).isNull();
    }

    @Test
    void testCreateCapturesNothing() {
        assertThat(apiKeyAuditMapper.capture(new AuditInvocation(
            ApiKeyAuditEvents.API_KEY_CREATED, Map.of("apiKey", createApiKey()), null, null, AuditOutcome.SUCCESS,
            null)))
                .isNull();
    }

    private static ApiKey createApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("automation key");
        apiKey.setType(PlatformType.AUTOMATION);

        return apiKey;
    }

    private static User newUser() {
        User user = new User();

        user.setActivated(true);
        user.setEmail("api-key-audit-mapper@localhost");
        user.setLangKey("en");
        user.setLogin("api-key-audit-mapper");
        user.setPassword("$2a$10$" + "a".repeat(53));
        user.setUuid(UUID.randomUUID());

        return user;
    }
}
