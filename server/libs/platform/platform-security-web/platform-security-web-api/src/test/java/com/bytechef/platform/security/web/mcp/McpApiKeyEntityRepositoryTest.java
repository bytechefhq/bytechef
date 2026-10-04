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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class McpApiKeyEntityRepositoryTest {

    private final ApiKeyService apiKeyService = mock(ApiKeyService.class);
    private final AuthorityService authorityService = mock(AuthorityService.class);
    private final UserService userService = mock(UserService.class);

    private final McpApiKeyEntityRepository mcpApiKeyEntityRepository = new McpApiKeyEntityRepository(
        apiKeyService, authorityService, userService);

    @Test
    void testFindByKeyIdReturnsEntityForActivatedUser() {
        ApiKey apiKey = getApiKey();

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(true);
        when(user.getLogin()).thenReturn("admin@localhost.com");
        when(user.getAuthorityIds()).thenReturn(List.of(5L));
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        Authority authority = mock(Authority.class);

        when(authority.getName()).thenReturn("ROLE_ADMIN");
        when(authorityService.fetchAuthority(5L)).thenReturn(Optional.of(authority));

        McpApiKeyEntity mcpApiKeyEntity = mcpApiKeyEntityRepository.findByKeyId("api-secret");

        assertThat(mcpApiKeyEntity).isNotNull();
        assertThat(mcpApiKeyEntity.getId()).isEqualTo("admin@localhost.com");
        assertThat(mcpApiKeyEntity.getSecret()).isEqualTo("{noop}api-secret");
        assertThat(mcpApiKeyEntity.getApiKeyId()).isEqualTo(7L);
        assertThat(mcpApiKeyEntity.getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(mcpApiKeyEntity.getEnvironment()).isEqualTo(Environment.PRODUCTION);
        assertThat(mcpApiKeyEntity.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testFindByKeyIdReturnsNullWhenApiKeyIsMissing() {
        when(apiKeyService.fetchApiKey("missing")).thenReturn(Optional.empty());

        assertThat(mcpApiKeyEntityRepository.findByKeyId("missing")).isNull();
    }

    @Test
    void testFindByKeyIdReturnsNullWhenUserIsNotActivated() {
        ApiKey apiKey = getApiKey();

        when(apiKeyService.fetchApiKey("api-secret")).thenReturn(Optional.of(apiKey));

        User user = mock(User.class);

        when(user.isActivated()).thenReturn(false);
        when(userService.fetchUser(100L)).thenReturn(Optional.of(user));

        assertThat(mcpApiKeyEntityRepository.findByKeyId("api-secret")).isNull();
    }

    private ApiKey getApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(7L);
        apiKey.setName("test");
        apiKey.setSecretKey("api-secret");
        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setEnvironment(Environment.PRODUCTION);
        apiKey.setUserId(100L);

        return apiKey;
    }
}
