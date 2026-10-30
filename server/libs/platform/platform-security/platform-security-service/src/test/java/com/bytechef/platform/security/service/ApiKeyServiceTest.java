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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;

/**
 * @author Ivica Cardic
 */
class ApiKeyServiceTest {

    private final ApiKeyRepository apiKeyRepository = mock(ApiKeyRepository.class);
    private final ApiKeyService apiKeyService = new ApiKeyServiceImpl(apiKeyRepository);

    @Test
    void testCreateGeneratesSecretKey() {
        when(apiKeyRepository.save(any(ApiKey.class))).then(AdditionalAnswers.returnsFirstArg());

        ApiKey apiKey = new ApiKey();

        apiKey.setName("automation key");
        apiKey.setType(PlatformType.AUTOMATION);

        ApiKey createdApiKey = apiKeyService.create(apiKey);

        assertThat(createdApiKey.getSecretKey()).isNotBlank();
    }

    @Test
    void testDelete() {
        apiKeyService.delete(7L);

        verify(apiKeyRepository).deleteById(7L);
    }
}
