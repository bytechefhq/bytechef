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
import com.bytechef.platform.security.service.ApiKeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
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
        OptionalLong workspaceId = workspaceApiKeyRepository.findByApiKeyId(id)
            .map(workspaceApiKey -> OptionalLong.of(workspaceApiKey.getWorkspaceId()))
            .orElse(OptionalLong.empty());

        OptionalLong ownerUserId = apiKeyService.fetchApiKey(id)
            .map(apiKey -> apiKey.getUserId())
            .map(OptionalLong::of)
            .orElse(OptionalLong.empty());

        return ResourceOwner.of(workspaceId, ownerUserId);
    }
}
