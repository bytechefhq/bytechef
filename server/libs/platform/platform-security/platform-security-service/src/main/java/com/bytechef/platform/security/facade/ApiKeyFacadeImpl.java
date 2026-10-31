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
import com.bytechef.platform.security.service.OwnedApiKeyCreator;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.ApiKeyRevoker;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * An API key authenticates as the user who owns it, carrying that user's authorities, so it is a second credential for
 * a person rather than a shared resource. Every read and write here is therefore restricted to the owner, with the
 * tenant admin as the single exception -- the same exception every other authorization check in the tree makes.
 *
 * <p>
 * Enforced in plain Java rather than through {@code @PreAuthorize}: the ownership SpEL functions are contributed by
 * {@code AutomationMethodSecurityConfiguration}, which is {@code @ConditionalOnBean(PermissionService.class)}. This
 * facade also serves deployments that carry no such bean, where a gate naming those functions would fail to evaluate
 * rather than deny. Ownership is knowable here without any of that machinery.
 *
 * <p>
 * Note that a workspace role cannot decide this. A key acts as its owner in every workspace they belong to, so a
 * workspace admin asked to approve it would be ruling on authority wider than their own -- and API keys carry no
 * workspace at all, being keyed on (user, environment, type).
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApiKeyFacadeImpl implements ApiKeyFacade, ApiKeyRevoker {

    private final ApiKeyService apiKeyService;
    private final OwnedApiKeyCreator ownedApiKeyCreator;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ApiKeyFacadeImpl(
        ApiKeyService apiKeyService, OwnedApiKeyCreator ownedApiKeyCreator, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.ownedApiKeyCreator = ownedApiKeyCreator;
        this.userService = userService;
    }

    /**
     * A null {@code type} mints a platform key -- the kind the Admin API Keys page issues, which authenticates against
     * the management surface rather than an automation or embedded one; an {@code EMBEDDED} key is what the Embedded
     * API Keys page issues. The schema leaves the argument open, so without this any authenticated caller could issue
     * either for themselves. Workspace-scoped automation keys go through
     * {@link #createOwnedApiKey(ApiKey, PlatformType)}.
     */
    @Override
    public ApiKey create(ApiKey apiKey, PlatformType type) {
        checkPlatformUser();

        if (!isTenantAdmin()) {
            throw new AccessDeniedException("Only a tenant admin may create this API key");
        }

        return ownedApiKeyCreator.create(apiKey, type);
    }

    @Override
    public ApiKey createOwnedApiKey(ApiKey apiKey, PlatformType type) {
        checkPlatformUser();

        return ownedApiKeyCreator.create(apiKey, type);
    }

    @Override
    public void delete(long id) {
        checkPlatformUser();

        getOwnedApiKey(id);

        apiKeyService.delete(id);
    }

    @Override
    public List<ApiKey> getAdminApiKeys(long environmentId) {
        checkPlatformUser();

        if (!isTenantAdmin()) {
            throw new AccessDeniedException("Only a tenant admin may list platform API keys");
        }

        return apiKeyService.getApiKeys(environmentId, null);
    }

    @Override
    public ApiKey getApiKey(long id) {
        checkPlatformUser();

        return getOwnedApiKey(id);
    }

    @Override
    public List<ApiKey> getApiKeys(long environmentId, PlatformType type) {
        checkPlatformUser();

        return filterOwned(apiKeyService.getApiKeys(environmentId, type));
    }

    @Override
    public ApiKey update(ApiKey apiKey) {
        checkPlatformUser();

        getOwnedApiKey(apiKey.getId());

        return apiKeyService.update(apiKey);
    }

    @Override
    public void revokeAll(long userId) {
        List<ApiKey> apiKeys = apiKeyService.getUserApiKeys(userId);

        for (ApiKey apiKey : apiKeys) {
            if (apiKey.getType() == PlatformType.EMBEDDED) {
                continue;
            }

            apiKeyService.delete(apiKey.getId());
        }
    }

    private static void checkPlatformUser() {
        if (NonPlatformUserAuthentication.isCurrent()) {
            throw new AccessDeniedException("Only a platform user has platform API keys");
        }
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
