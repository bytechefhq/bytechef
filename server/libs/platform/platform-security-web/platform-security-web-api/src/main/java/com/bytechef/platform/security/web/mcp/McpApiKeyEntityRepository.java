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

package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.security.server.apikey.ApiKeyEntityRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Looks up ByteChef API keys by their opaque secret. Runs inside the tenant context established by
 * {@link TenantAwareApiKeyAuthenticationFilter}.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyEntityRepository implements ApiKeyEntityRepository<McpApiKeyEntity> {

    private final ApiKeyService apiKeyService;
    private final AuthorityService authorityService;
    private final UserService userService;

    @SuppressFBWarnings("EI2")
    public McpApiKeyEntityRepository(
        ApiKeyService apiKeyService, AuthorityService authorityService, UserService userService) {

        this.apiKeyService = apiKeyService;
        this.authorityService = authorityService;
        this.userService = userService;
    }

    @Override
    @Nullable
    public McpApiKeyEntity findByKeyId(String keyId) {
        return apiKeyService.fetchApiKey(keyId)
            .flatMap(apiKey -> userService.fetchUser(apiKey.getUserId())
                .filter(User::isActivated)
                .map(user -> new McpApiKeyEntity(apiKey, user.getLogin(), getGrantedAuthorities(user))))
            .orElse(null);
    }

    private List<GrantedAuthority> getGrantedAuthorities(User user) {
        return user.getAuthorityIds()
            .stream()
            .map(authorityService::fetchAuthority)
            .flatMap(Optional::stream)
            .map(Authority::getName)
            .map(authorityName -> (GrantedAuthority) new SimpleGrantedAuthority(authorityName))
            .toList();
    }
}
