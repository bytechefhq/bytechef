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

package com.bytechef.platform.security.facade;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentications;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiKeyFacadeImpl implements ApiKeyFacade {

    private final ApiKeyService apiKeyService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ApiKeyFacadeImpl(ApiKeyService apiKeyService, UserService userService) {
        this.apiKeyService = apiKeyService;
        this.userService = userService;
    }

    @Override
    public ApiKey create(ApiKey apiKey, PlatformType type) {
        checkNotConnectedUser();

        if (!isTenantAdmin()) {
            throw new AccessDeniedException("Only a tenant admin may create this API key");
        }

        return createOwnedApiKey(apiKey, type);
    }

    @Override
    public ApiKey createAutomationApiKey(ApiKey apiKey) {
        checkNotConnectedUser();

        return createOwnedApiKey(apiKey, PlatformType.AUTOMATION);
    }

    @Override
    public void delete(long id) {
        checkNotConnectedUser();

        getOwnedApiKey(id);

        apiKeyService.delete(id);
    }

    @Override
    public List<ApiKey> getAdminApiKeys(long environmentId) {
        checkNotConnectedUser();

        if (!isTenantAdmin()) {
            throw new AccessDeniedException("Only a tenant admin may list platform API keys");
        }

        return apiKeyService.getApiKeys(environmentId, null);
    }

    @Override
    public ApiKey getApiKey(long id) {
        checkNotConnectedUser();

        return getOwnedApiKey(id);
    }

    @Override
    public List<ApiKey> getApiKeys(long environmentId, PlatformType type) {
        checkNotConnectedUser();

        return filterOwned(apiKeyService.getApiKeys(environmentId, type));
    }

    @Override
    public ApiKey update(ApiKey apiKey) {
        checkNotConnectedUser();

        getOwnedApiKey(apiKey.getId());

        return apiKeyService.update(apiKey);
    }

    private static void checkNotConnectedUser() {
        if (ConnectedUserAuthentications.isConnectedUser()) {
            throw new AccessDeniedException("A connected user has no platform API keys");
        }
    }

    private ApiKey createOwnedApiKey(ApiKey apiKey, PlatformType type) {
        User user = userService.getCurrentUser();

        apiKey.setType(type);
        apiKey.setUserId(user.getId());

        return apiKeyService.create(apiKey);
    }

    private List<ApiKey> filterOwned(List<ApiKey> apiKeys) {
        if (isTenantAdmin()) {
            return apiKeys;
        }

        Long currentUserId = fetchCurrentUserId();

        return apiKeys.stream()
            .filter(apiKey -> Objects.equals(apiKey.getUserId(), currentUserId))
            .toList();
    }

    private ApiKey getOwnedApiKey(long id) {
        ApiKey apiKey = apiKeyService.getApiKey(id);

        if (isTenantAdmin() || Objects.equals(apiKey.getUserId(), fetchCurrentUserId())) {
            return apiKey;
        }

        throw new IllegalArgumentException("Api key not found for id: " + id);
    }

    @Nullable
    private Long fetchCurrentUserId() {
        return SecurityUtils.fetchCurrentUserLogin()
            .flatMap(userService::fetchUserByLogin)
            .map(User::getId)
            .orElse(null);
    }

    private static boolean isTenantAdmin() {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }
}
