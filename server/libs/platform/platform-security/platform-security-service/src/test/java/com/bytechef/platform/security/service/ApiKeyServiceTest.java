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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.audit.ApiKeyAuditPublisher;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import java.util.Optional;
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
    void testFetchApiKeyReturnsMatchingApiKey() {
        ApiKey apiKey = new ApiKey();

        when(apiKeyRepository.findBySecretKey("secret")).thenReturn(Optional.of(apiKey));

        assertThat(apiKeyService.fetchApiKey("secret")).contains(apiKey);
    }

    @Test
    void testFetchApiKeyReturnsEmptyWhenMissing() {
        when(apiKeyRepository.findBySecretKey("missing")).thenReturn(Optional.empty());

        assertThat(apiKeyService.fetchApiKey("missing")).isEmpty();
    }

    @Test
    void testUpdateLastUsedDateSetsTimestampAndSaves() {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(17L);

        when(apiKeyRepository.findById(17L)).thenReturn(Optional.of(apiKey));

        apiKeyService.updateLastUsedDate(17L);

        ArgumentCaptor<ApiKey> apiKeyArgumentCaptor = ArgumentCaptor.forClass(ApiKey.class);

        verify(apiKeyRepository).save(apiKeyArgumentCaptor.capture());

        ApiKey savedApiKey = apiKeyArgumentCaptor.getValue();

        assertThat(savedApiKey.getLastUsedDate()).isNotNull();
    }
}
