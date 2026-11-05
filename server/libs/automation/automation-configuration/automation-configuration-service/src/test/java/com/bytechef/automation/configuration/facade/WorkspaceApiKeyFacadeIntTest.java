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

package com.bytechef.automation.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.config.ProjectIntTestConfiguration;
import com.bytechef.automation.configuration.config.WorkspaceApiKeyIntTestConfigurationSharedMocks;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceApiKey;
import com.bytechef.automation.configuration.repository.WorkspaceApiKeyRepository;
import com.bytechef.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = ProjectIntTestConfiguration.class)
@WorkspaceApiKeyIntTestConfigurationSharedMocks
class WorkspaceApiKeyFacadeIntTest {

    private static final long ENVIRONMENT_ID = Environment.DEVELOPMENT.ordinal();
    private static final String OTHER_LOGIN = "other@localhost.com";
    private static final long OTHER_USER_ID = 2L;
    private static final String OWNER_LOGIN = "owner@localhost.com";
    private static final long OWNER_USER_ID = 1L;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private WorkspaceApiKeyFacade workspaceApiKeyFacade;

    @Autowired
    private WorkspaceApiKeyRepository workspaceApiKeyRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("api-key-facade"));

        workspaceId = workspace.getId();
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        workspaceApiKeyRepository.deleteAll();
        apiKeyRepository.deleteAll();
        workspaceRepository.deleteById(workspaceId);
    }

    @Test
    void testCreateDeniesAnAnonymousCaller() {
        authenticateAnonymous();

        assertThatThrownBy(() -> workspaceApiKeyFacade.create(workspaceId, newApiKey()))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiKeyRepository.findAll()).isEmpty();
        assertThat(workspaceApiKeyRepository.findAllByWorkspaceId(workspaceId)).isEmpty();

        verify(permissionService).hasWorkspaceScope(workspaceId, "API_KEY_CREATE", Environment.DEVELOPMENT);
    }

    @Test
    void testCreateMintsAnAutomationKeyOwnedByTheCallerAndLinksItToTheWorkspace() {
        authenticate(OWNER_USER_ID, OWNER_LOGIN, AuthorityConstants.USER);

        String secretKey = workspaceApiKeyFacade.create(workspaceId, newApiKey());

        Optional<ApiKey> createdApiKey = apiKeyRepository.findBySecretKey(secretKey);

        assertThat(createdApiKey).hasValueSatisfying(apiKey -> {
            assertThat(apiKey.getUserId()).isEqualTo(OWNER_USER_ID);
            assertThat(apiKey.getType()).isEqualTo(PlatformType.AUTOMATION);
            assertThat(workspaceApiKeyRepository.findByApiKeyId(apiKey.getId()))
                .map(WorkspaceApiKey::getWorkspaceId)
                .hasValue(workspaceId);
        });

        verify(permissionService).hasWorkspaceScope(workspaceId, "API_KEY_CREATE", Environment.DEVELOPMENT);
    }

    @Test
    void testGetApiKeysDeniesAnAnonymousCaller() {
        ApiKey apiKey = createApiKey(OWNER_USER_ID);

        workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKey.getId(), workspaceId));

        authenticateAnonymous();

        assertThatThrownBy(() -> workspaceApiKeyFacade.getApiKeys(workspaceId, ENVIRONMENT_ID))
            .isInstanceOf(AccessDeniedException.class);

        verify(permissionService).hasWorkspaceScope(workspaceId, "API_KEY_VIEW", Environment.DEVELOPMENT);
    }

    @Test
    void testGetApiKeysListsOnlyTheCallerVisibleKeysOfTheWorkspace() {
        ApiKey ownerBoundApiKey = createApiKey(OWNER_USER_ID);
        ApiKey otherBoundApiKey = createApiKey(OTHER_USER_ID);

        createApiKey(OWNER_USER_ID);

        workspaceApiKeyRepository.save(new WorkspaceApiKey(ownerBoundApiKey.getId(), workspaceId));
        workspaceApiKeyRepository.save(new WorkspaceApiKey(otherBoundApiKey.getId(), workspaceId));

        authenticate(OWNER_USER_ID, OWNER_LOGIN, AuthorityConstants.USER);

        assertThat(workspaceApiKeyFacade.getApiKeys(workspaceId, ENVIRONMENT_ID))
            .extracting(ApiKey::getId)
            .containsExactly(ownerBoundApiKey.getId());

        verify(permissionService).hasWorkspaceScope(workspaceId, "API_KEY_VIEW", Environment.DEVELOPMENT);
    }

    @Test
    void testDeleteDeniesAMemberWhoDoesNotOwnTheKey() {
        ApiKey apiKey = createApiKey(OWNER_USER_ID);

        workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKey.getId(), workspaceId));

        authenticate(OTHER_USER_ID, OTHER_LOGIN, AuthorityConstants.USER);

        long apiKeyId = apiKey.getId();

        assertThatThrownBy(() -> workspaceApiKeyFacade.delete(apiKeyId))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiKeyRepository.findById(apiKeyId)).isPresent();
        assertThat(workspaceApiKeyRepository.findByApiKeyId(apiKeyId)).isPresent();

        verify(permissionService).hasResourceScope(apiKeyId, "ApiKey", "API_KEY_DELETE");
    }

    @Test
    void testDeleteRemovesTheCallerOwnKey() {
        ApiKey apiKey = createApiKey(OWNER_USER_ID);

        workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKey.getId(), workspaceId));

        authenticate(OWNER_USER_ID, OWNER_LOGIN, AuthorityConstants.USER);

        long apiKeyId = apiKey.getId();

        workspaceApiKeyFacade.delete(apiKeyId);

        assertThat(apiKeyRepository.findById(apiKeyId)).isEmpty();
        assertThat(workspaceApiKeyRepository.findByApiKeyId(apiKeyId)).isEmpty();

        verify(permissionService).hasResourceScope(apiKeyId, "ApiKey", "API_KEY_DELETE");
    }

    @Test
    void testDeleteRefusesAnotherUserKeyBeforeTouchingItWhenTheGateIsSkipped() {
        ApiKey apiKey = createApiKey(OWNER_USER_ID);

        workspaceApiKeyRepository.save(new WorkspaceApiKey(apiKey.getId(), workspaceId));

        authenticate(OTHER_USER_ID, OTHER_LOGIN, AuthorityConstants.USER);

        long apiKeyId = apiKey.getId();

        assertThatThrownBy(() -> AutomationAuthorizationContext.callSkippingChecks(() -> {
            workspaceApiKeyFacade.delete(apiKeyId);

            return null;
        }))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(apiKeyRepository.findById(apiKeyId)).isPresent();
        assertThat(workspaceApiKeyRepository.findByApiKeyId(apiKeyId)).isPresent();
    }

    private void authenticate(long userId, String login, String authority) {
        User user = new User();

        user.setId(userId);
        user.setLogin(login);

        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));
        when(userService.getCurrentUser()).thenReturn(user);
        when(userService.fetchUserByLogin(login)).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    login, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private static void authenticateAnonymous() {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new AnonymousAuthenticationToken(
                    "anonymous", "anonymousUser", List.of(new SimpleGrantedAuthority(AuthorityConstants.ANONYMOUS))));
    }

    private ApiKey createApiKey(long ownerUserId) {
        ApiKey apiKey = newApiKey();

        apiKey.setType(PlatformType.AUTOMATION);
        apiKey.setUserId(ownerUserId);

        return apiKeyService.create(apiKey);
    }

    private static ApiKey newApiKey() {
        ApiKey apiKey = new ApiKey();

        apiKey.setName("workspace key");

        return apiKey;
    }
}
