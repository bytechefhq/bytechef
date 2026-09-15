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
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public ApiKeyFacadeImpl(ApiKeyService apiKeyService, UserService userService) {
        this.apiKeyService = apiKeyService;
        this.userService = userService;
    }

    /**
     * A null {@code type} mints a platform key -- the kind the Admin API Keys page issues, which authenticates against
     * the management surface rather than an automation or embedded one. The schema leaves the argument nullable, so
     * without this any authenticated caller could issue one for themselves.
     */
    @Override
    public ApiKey create(ApiKey apiKey, PlatformType type) {
        if (type == null && !isTenantAdmin()) {
            throw new AccessDeniedException("Only a tenant admin may create a platform API key");
        }

        User user = userService.getCurrentUser();

        apiKey.setType(type);
        apiKey.setUserId(user.getId());

        return apiKeyService.create(apiKey);
    }

    @Override
    public void delete(long id) {
        getOwnedApiKey(id);

        apiKeyService.delete(id);
    }

    /**
     * Tenant admin only, rather than owner-filtered like the ordinary listing. These are the platform keys, and the
     * page that shows them is an administrative one: a member has no business enumerating that surface at all, even
     * though the filter would hand them nothing but their own.
     */
    @Override
    public List<ApiKey> getAdminApiKeys(long environmentId) {
        if (!isTenantAdmin()) {
            throw new AccessDeniedException("Only a tenant admin may list platform API keys");
        }

        return apiKeyService.getApiKeys(environmentId, null);
    }

    @Override
    public ApiKey getApiKey(long id) {
        return getOwnedApiKey(id);
    }

    @Override
    public List<ApiKey> getApiKeys(long environmentId, PlatformType type) {
        return filterOwned(apiKeyService.getApiKeys(environmentId, type));
    }

    @Override
    public ApiKey update(ApiKey apiKey) {
        getOwnedApiKey(apiKey.getId());

        return apiKeyService.update(apiKey);
    }

    /**
     * Revokes every key a departing user owns. Deliberately not owner-filtered: the keys belong to the account being
     * deleted, never to the caller. Reaching it requires {@code UserManagementFacade.deleteUser}, which is
     * {@code ADMIN}-only, and that is the whole of its authorization.
     *
     * <p>
     * Key by key through the service rather than a bulk delete, so each revocation lands on the audit trail as an
     * ordinary {@code API_KEY_DELETED}. A departing user's credentials are exactly the ones somebody asks about later.
     */
    @Override
    public void revokeAll(long userId) {
        List<ApiKey> apiKeys = apiKeyService.getUserApiKeys(userId);

        for (ApiKey apiKey : apiKeys) {
            apiKeyService.delete(apiKey.getId());
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

    /**
     * A key belonging to somebody else answers "not found" rather than "forbidden". The ids are sequential, so the
     * difference between those two answers is an oracle for who holds machine access to which environment -- and the
     * caller has no legitimate use for the distinction, since neither answer gives them the key.
     */
    private ApiKey getOwnedApiKey(long id) {
        ApiKey apiKey = apiKeyService.getApiKey(id);

        if (isTenantAdmin() || Objects.equals(apiKey.getUserId(), fetchCurrentUserId())) {
            return apiKey;
        }

        throw new IllegalArgumentException("Api key not found for id: " + id);
    }

    /**
     * Null rather than an exception when nobody is authenticated: the comparison against an owner id then fails, which
     * is the same denial the check would otherwise have to spell out.
     */
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
