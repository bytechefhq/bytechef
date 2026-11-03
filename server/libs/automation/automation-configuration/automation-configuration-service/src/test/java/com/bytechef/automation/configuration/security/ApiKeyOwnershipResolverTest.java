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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApiKeyOwnershipResolverTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final WorkspaceApiKeyRepository workspaceApiKeyRepository = mock(WorkspaceApiKeyRepository.class);
    private final ApiKeyOwnershipResolver resolver =
        new ApiKeyOwnershipResolver(workspaceApiKeyRepository, apiKeyService);

    private void stubApiKey(PlatformType type) {
        ApiKey apiKey = new ApiKey();

        apiKey.setType(type);
        apiKey.setUserId(5L);

        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.of(apiKey));
    }

    @Test
    void testResolveOwnerReturnsTheOwnerOfAnAutomationKey() {
        stubApiKey(PlatformType.AUTOMATION);

        when(workspaceApiKeyRepository.findByApiKeyId(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.ofUser(5L));
    }

    @Test
    void testAutomationApiKeyResolverIgnoresEmbeddedRows() {
        stubApiKey(PlatformType.EMBEDDED);

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }

    @Test
    void testResolveOwnerFailsClosedWhenTheKeyDoesNotExist() {
        when(apiKeyService.fetchApiKey(7L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(7L)).isEqualTo(ResourceOwner.unknown());
    }
}
