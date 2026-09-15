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
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApiKeyServiceTest {

    private static final String SECRET_KEY = "secret-key";

    @Test
    void testExistsMatchesOnlyAKeyOfTheRequestedType() {
        ApiKeyRepository apiKeyRepository = mock(ApiKeyRepository.class);
        ApiKey automationApiKey = new ApiKey();

        automationApiKey.setType(PlatformType.AUTOMATION);

        when(apiKeyRepository.findBySecretKeyAndEnvironment(SECRET_KEY, 1))
            .thenReturn(Optional.of(automationApiKey));
        when(apiKeyRepository.existsBySecretKeyAndEnvironmentAndType(
            SECRET_KEY, 1, PlatformType.AUTOMATION.ordinal())).thenReturn(true);

        ApiKeyService apiKeyService = new ApiKeyServiceImpl(apiKeyRepository);

        assertThat(apiKeyService.exists(SECRET_KEY, 1L, PlatformType.EMBEDDED)).isFalse();
        assertThat(apiKeyService.exists(SECRET_KEY, 1L, PlatformType.AUTOMATION)).isTrue();
    }
}
