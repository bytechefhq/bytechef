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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApiKeyEnvironmentResolverTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final ApiKeyEnvironmentResolver resolver = new ApiKeyEnvironmentResolver(apiKeyService);

    @Test
    void testResourceTypeMatchesTheTokenTheApiKeyGuardsName() {
        assertThat(resolver.resourceType()).isEqualTo("ApiKey");
    }

    @Test
    void testFetchEnvironmentReturnsTheEnvironmentTheApiKeyWasIssuedIn() {
        ApiKey apiKey = new ApiKey();

        apiKey.setEnvironment(Environment.PRODUCTION);

        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.of(apiKey));

        assertThat(resolver.fetchEnvironment(7L)).contains(Environment.PRODUCTION);
    }

    @Test
    void testFetchEnvironmentIsEmptyWhenTheApiKeyDoesNotExist() {
        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.empty());

        assertThat(resolver.fetchEnvironment(7L)).isEmpty();
    }

    @Test
    void testFetchEnvironmentIsEmptyForANonNumericId() {
        assertThat(resolver.fetchEnvironment("7")).isEmpty();

        verifyNoInteractions(apiKeyService);
    }
}
