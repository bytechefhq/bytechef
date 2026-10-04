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

package com.bytechef.platform.security.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.audit.ApiKeyAuditEvent;
import com.bytechef.platform.security.audit.ApiKeyAuditPublisher;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
class ApiKeyServiceTest {

    private final ApiKeyAuditPublisher apiKeyAuditPublisher = mock(ApiKeyAuditPublisher.class);
    private final ApiKeyRepository apiKeyRepository = mock(ApiKeyRepository.class);
    private final ApiKeyService apiKeyService = new ApiKeyServiceImpl(apiKeyAuditPublisher, apiKeyRepository);

    @Test
    @SuppressWarnings("unchecked")
    void testCreateAuditDataOmitsSecretKey() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey apiKey = invocation.getArgument(0);

            apiKey.setId(7L);

            return apiKey;
        });

        ApiKey apiKey = new ApiKey();

        apiKey.setName("automation key");
        apiKey.setType(PlatformType.AUTOMATION);

        ApiKey createdApiKey = apiKeyService.create(apiKey);

        ArgumentCaptor<Map<String, Object>> dataArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(apiKeyAuditPublisher).publish(
            eq(ApiKeyAuditEvent.API_KEY_CREATED), eq(7L), dataArgumentCaptor.capture());

        Map<String, Object> data = dataArgumentCaptor.getValue();

        assertThat(createdApiKey.getSecretKey()).isNotBlank();
        assertThat(data).containsOnly(Map.entry("name", "automation key"), Map.entry("type", "AUTOMATION"));
        assertThat(data.values()).doesNotContain(createdApiKey.getSecretKey());
    }

    @Test
    void testDeletePublishesAuditEventWithoutAdditionalData() {
        apiKeyService.delete(7L);

        verify(apiKeyRepository).deleteById(7L);
        verify(apiKeyAuditPublisher).publish(ApiKeyAuditEvent.API_KEY_DELETED, 7L);
    }
}
