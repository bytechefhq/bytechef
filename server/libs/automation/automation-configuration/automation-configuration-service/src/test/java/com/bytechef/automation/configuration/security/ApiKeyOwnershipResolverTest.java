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
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApiKeyOwnershipResolverTest {

    private final WorkspaceApiKeyRepository workspaceApiKeyRepository = Mockito.mock(WorkspaceApiKeyRepository.class);
    private final ApiKeyService apiKeyService = Mockito.mock(ApiKeyService.class);

    private final ApiKeyOwnershipResolver resolver =
        new ApiKeyOwnershipResolver(workspaceApiKeyRepository, apiKeyService);

    @Test
    void testResourceType() {
        assertThat(resolver.resourceType()).isEqualTo("ApiKey");
    }

    @Test
    void testResolvesWorkspaceAndOwner() {
        when(workspaceApiKeyRepository.findByApiKeyId(1L)).thenReturn(Optional.of(new WorkspaceApiKey(1L, 42L)));

        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(7L);

        when(apiKeyService.fetchApiKey(1L)).thenReturn(Optional.of(apiKey));

        assertThat(resolver.resolveOwner(1L)
            .workspaceId()).hasValue(42L);
        assertThat(resolver.resolveOwner(1L)
            .ownerUserId()).hasValue(7L);
    }

    @Test
    void testPersonalKeyHasOwnerButNoWorkspace() {
        when(workspaceApiKeyRepository.findByApiKeyId(2L)).thenReturn(Optional.empty());

        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(7L);

        when(apiKeyService.fetchApiKey(2L)).thenReturn(Optional.of(apiKey));

        assertThat(resolver.resolveOwner(2L)
            .workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner(2L)
            .ownerUserId()).hasValue(7L);
    }

    @Test
    void testUnknownApiKeyIsUnknown() {
        when(workspaceApiKeyRepository.findByApiKeyId(99L)).thenReturn(Optional.empty());
        when(apiKeyService.fetchApiKey(99L)).thenReturn(Optional.empty());

        assertThat(resolver.resolveOwner(99L)
            .workspaceId()).isEmpty();
        assertThat(resolver.resolveOwner(99L)
            .ownerUserId()).isEmpty();
    }
}
