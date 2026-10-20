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

import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps an API-key id to its owning workspace (via the {@code workspace_api_key} relation, when present) and to the
 * key's owner ({@code api_key.user_id}). A personal API key has no workspace mapping but always has an owner; a
 * workspace API key has both. EE personal-key checks authorize by owner; EE workspace-key checks authorize by workspace
 * scope; CE workspace-key checks authorize by owner-isolation. Fails closed when the key does not exist.
 *
 * @author Ivica Cardic
 */
@Component
public class ApiKeyOwnershipResolver implements ResourceOwnershipResolver {

    private final WorkspaceApiKeyRepository workspaceApiKeyRepository;
    private final ApiKeyService apiKeyService;

    @SuppressFBWarnings("EI")
    public ApiKeyOwnershipResolver(
        WorkspaceApiKeyRepository workspaceApiKeyRepository, ApiKeyService apiKeyService) {

        this.workspaceApiKeyRepository = workspaceApiKeyRepository;
        this.apiKeyService = apiKeyService;
    }

    @Override
    public String resourceType() {
        return "ApiKey";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<ApiKey> apiKeyOptional = apiKeyService.fetchApiKey(id);

        if (apiKeyOptional.isEmpty()) {
            return ResourceOwner.unknown();
        }

        ApiKey apiKey = apiKeyOptional.get();

        PlatformType type = apiKey.getType();

        if (type != null && type != PlatformType.AUTOMATION) {
            return ResourceOwner.unknown();
        }

        OptionalLong workspaceId = workspaceApiKeyRepository.findByApiKeyId(id)
            .map(workspaceApiKey -> OptionalLong.of(workspaceApiKey.getWorkspaceId()))
            .orElse(OptionalLong.empty());

        Long userId = apiKey.getUserId();

        OptionalLong ownerUserId = userId == null ? OptionalLong.empty() : OptionalLong.of(userId);

        return ResourceOwner.of(workspaceId, ownerUserId);
    }
}
