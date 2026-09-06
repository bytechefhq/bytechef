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

package com.bytechef.ai.mcp.server.security.web.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.service.PropertyService;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class ManagementMcpServerApiKeyAuthenticationProviderTest {

    private static final String AUTH_SECRET_KEY = "api-secret";
    private static final long AUTHORITY_ID = 1L;
    private static final String MCP_SERVER_SECRET_KEY = "mcp-server-secret";
    private static final long USER_ID = 7L;

    private ApiKeyService apiKeyService;
    private AuthorityService authorityService;
    private PropertyService propertyService;
    private ManagementMcpServerApiKeyAuthenticationProvider provider;
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        apiKeyService = mock(ApiKeyService.class);
        authorityService = mock(AuthorityService.class);
        propertyService = mock(PropertyService.class);
        userService = mock(UserService.class);

        provider = new ManagementMcpServerApiKeyAuthenticationProvider(
            apiKeyService, authorityService, propertyService, userService);
    }

    @Test
    void testAuthenticateRejectsWhenMcpServerPropertyIsMissing() {
        when(propertyService.fetchProperty(eq("mcp.server"), eq(Property.Scope.PLATFORM), isNull()))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(token(MCP_SERVER_SECRET_KEY, null)))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void testAuthenticateRejectsWhenMcpServerSecretKeyDoesNotMatch() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);

        assertThatThrownBy(() -> provider.authenticate(token("wrong-secret", null)))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void testAuthenticateReturnsAuthenticatedTokenWhenOnlyMcpServerSecretKeyMatches() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);

        Authentication authentication = provider.authenticate(token(MCP_SERVER_SECRET_KEY, null));

        assertThat(authentication.isAuthenticated()).isTrue();

        verifyNoInteractions(apiKeyService);
    }

    @Test
    void testAuthenticateReturnsUserTokenWhenApiKeyIsValid() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);
        stubApiKey();
        stubUser(true);

        Authority authority = new Authority();

        authority.setId(AUTHORITY_ID);
        authority.setName("ROLE_ADMIN");

        when(authorityService.fetchAuthority(AUTHORITY_ID)).thenReturn(Optional.of(authority));

        Authentication authentication = provider.authenticate(token(MCP_SERVER_SECRET_KEY, AUTH_SECRET_KEY));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
            .isInstanceOfSatisfying(
                org.springframework.security.core.userdetails.User.class,
                user -> assertThat(user.getUsername()).isEqualTo("admin"));
        assertThat(authentication.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_ADMIN");
    }

    @Test
    void testAuthenticateRejectsWhenApiKeyIsInvalid() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);

        when(apiKeyService.getApiKey(AUTH_SECRET_KEY)).thenThrow(new IllegalArgumentException("unknown"));

        assertThatThrownBy(() -> provider.authenticate(token(MCP_SERVER_SECRET_KEY, AUTH_SECRET_KEY)))
            .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userService);
    }

    @Test
    void testAuthenticateRejectsWhenApiKeyUserDoesNotExist() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);
        stubApiKey();

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(token(MCP_SERVER_SECRET_KEY, AUTH_SECRET_KEY)))
            .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void testAuthenticateRejectsWhenApiKeyUserIsNotActivated() {
        stubMcpServerProperty(MCP_SERVER_SECRET_KEY);
        stubApiKey();
        stubUser(false);

        assertThatThrownBy(() -> provider.authenticate(token(MCP_SERVER_SECRET_KEY, AUTH_SECRET_KEY)))
            .isInstanceOf(UserNotActivatedException.class);

        verifyNoInteractions(authorityService);
    }

    private void stubApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setUserId(USER_ID);

        when(apiKeyService.getApiKey(AUTH_SECRET_KEY)).thenReturn(apiKey);
    }

    private void stubMcpServerProperty(String secretKey) {
        Property property = new Property();

        property.setValue(Map.of("secretKey", secretKey));

        when(propertyService.fetchProperty(eq("mcp.server"), eq(Property.Scope.PLATFORM), isNull()))
            .thenReturn(Optional.of(property));
    }

    private void stubUser(boolean activated) {
        User user = new User();

        user.setId(USER_ID);
        user.setLogin("admin");
        user.setPassword("password");
        user.setActivated(activated);
        user.setAuthorityIds(List.of(AUTHORITY_ID));

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));
    }

    private static ManagementMcpServerApiKeyAuthenticationToken token(String mcpServerSecretKey, String authSecretKey) {
        return new ManagementMcpServerApiKeyAuthenticationToken(mcpServerSecretKey, authSecretKey, "public");
    }
}
