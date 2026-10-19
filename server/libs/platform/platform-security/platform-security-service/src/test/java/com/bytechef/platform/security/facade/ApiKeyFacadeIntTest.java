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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.config.PlatformSecurityIntTestConfiguration;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.repository.ApiKeyRepository;
import com.bytechef.platform.security.web.authentication.ConnectedUserAuthentication;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformSecurityIntTestConfiguration.class)
class ApiKeyFacadeIntTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final String OTHER_LOGIN = "other-user";
    private static final String OWNER_LOGIN = "owner-user";

    @Autowired
    private ApiKeyFacade apiKeyFacade;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    private User otherUser;
    private User ownerUser;

    @BeforeEach
    void beforeEach() {
        ownerUser = userRepository.save(newUser(OWNER_LOGIN));
        otherUser = userRepository.save(newUser(OTHER_LOGIN));

        authenticateAs(OWNER_LOGIN, false);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();

        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testGetApiKeysReturnsOnlyTheCallerOwnKeys() {
        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);

        saveApiKey("other key", otherUser, PlatformType.AUTOMATION);

        assertThat(apiKeyFacade.getApiKeys(ENVIRONMENT_ID, PlatformType.AUTOMATION))
            .extracting(ApiKey::getId)
            .containsExactly(ownerApiKey.getId());
    }

    @Test
    void testGetApiKeysReturnsEveryKeyForTheTenantAdmin() {
        authenticateAs(OWNER_LOGIN, true);

        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);
        ApiKey otherApiKey = saveApiKey("other key", otherUser, PlatformType.AUTOMATION);

        assertThat(apiKeyFacade.getApiKeys(ENVIRONMENT_ID, PlatformType.AUTOMATION))
            .extracting(ApiKey::getId)
            .containsExactlyInAnyOrder(ownerApiKey.getId(), otherApiKey.getId());
    }

    @Test
    void testGetApiKeyAnswersNotFoundForSomebodyElseKey() {
        ApiKey otherApiKey = saveApiKey("other key", otherUser, PlatformType.AUTOMATION);

        assertThatThrownBy(() -> apiKeyFacade.getApiKey(otherApiKey.getId()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Api key not found for id: " + otherApiKey.getId());
    }

    @Test
    void testGetApiKeyReturnsTheCallerOwnKey() {
        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);

        ApiKey apiKey = apiKeyFacade.getApiKey(ownerApiKey.getId());

        assertThat(apiKey.getId()).isEqualTo(ownerApiKey.getId());
        assertThat(apiKey.getName()).isEqualTo("owner key");
        assertThat(apiKey.getUserId()).isEqualTo(ownerUser.getId());
    }

    @Test
    void testTenantAdminReadsAnyKey() {
        authenticateAs(OWNER_LOGIN, true);

        ApiKey otherApiKey = saveApiKey("other key", otherUser, PlatformType.AUTOMATION);

        ApiKey apiKey = apiKeyFacade.getApiKey(otherApiKey.getId());

        assertThat(apiKey.getId()).isEqualTo(otherApiKey.getId());
        assertThat(apiKey.getUserId()).isEqualTo(otherUser.getId());
    }

    @Test
    void testDeleteRefusesSomebodyElseKeyWithoutTouchingIt() {
        ApiKey otherApiKey = saveApiKey("other key", otherUser, PlatformType.AUTOMATION);

        assertThatThrownBy(() -> apiKeyFacade.delete(otherApiKey.getId()))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(apiKeyRepository.findById(otherApiKey.getId())).isPresent();
    }

    @Test
    void testDeleteRemovesTheCallerOwnKey() {
        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);

        apiKeyFacade.delete(ownerApiKey.getId());

        assertThat(apiKeyRepository.findById(ownerApiKey.getId())).isEmpty();
    }

    @Test
    void testUpdateRefusesSomebodyElseKey() {
        ApiKey otherApiKey = saveApiKey("other key", otherUser, PlatformType.AUTOMATION);

        ApiKey renamedApiKey = renamed(otherApiKey.getId(), "renamed");

        assertThatThrownBy(() -> apiKeyFacade.update(renamedApiKey))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(apiKeyRepository.findById(otherApiKey.getId()))
            .hasValueSatisfying(apiKey -> assertThat(apiKey.getName()).isEqualTo("other key"));
    }

    @Test
    void testUpdateRenamesTheCallerOwnKey() {
        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);

        apiKeyFacade.update(renamed(ownerApiKey.getId(), "renamed"));

        assertThat(apiKeyRepository.findById(ownerApiKey.getId()))
            .hasValueSatisfying(apiKey -> assertThat(apiKey.getName()).isEqualTo("renamed"));
    }

    @Test
    void testAnUnauthenticatedCallerOwnsNothing() {
        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);

        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> apiKeyFacade.getApiKey(ownerApiKey.getId()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testOnlyTheTenantAdminMayCreateAPlatformKey() {
        assertThatThrownBy(() -> apiKeyFacade.create(newApiKey("platform key"), null))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiKeyRepository.findAll()).isEmpty();

        authenticateAs(OWNER_LOGIN, true);

        ApiKey createdApiKey = apiKeyFacade.create(newApiKey("platform key"), null);

        assertThat(apiKeyRepository.findById(createdApiKey.getId()))
            .hasValueSatisfying(apiKey -> {
                assertThat(apiKey.getType()).isNull();
                assertThat(apiKey.getUserId()).isEqualTo(ownerUser.getId());
            });
    }

    @Test
    void testOnlyTheTenantAdminMayCreateAnEmbeddedKey() {
        assertThatThrownBy(() -> apiKeyFacade.create(newApiKey("embedded key"), PlatformType.EMBEDDED))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiKeyRepository.findAll()).isEmpty();

        authenticateAs(OWNER_LOGIN, true);

        ApiKey createdApiKey = apiKeyFacade.create(newApiKey("embedded key"), PlatformType.EMBEDDED);

        assertThat(apiKeyRepository.findById(createdApiKey.getId()))
            .hasValueSatisfying(apiKey -> {
                assertThat(apiKey.getType()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(apiKey.getUserId()).isEqualTo(ownerUser.getId());
            });
    }

    @Test
    void testAMemberMayNotCreateAnUnscopedAutomationKey() {
        assertThatThrownBy(() -> apiKeyFacade.create(newApiKey("automation key"), PlatformType.AUTOMATION))
            .isInstanceOf(AccessDeniedException.class);

        assertThat(apiKeyRepository.findAll()).isEmpty();
    }

    @Test
    void testAMemberMayCreateAWorkspaceAutomationKey() {
        ApiKey createdApiKey = apiKeyFacade.createAutomationApiKey(newApiKey("workspace key"));

        assertThat(apiKeyRepository.findById(createdApiKey.getId()))
            .hasValueSatisfying(apiKey -> {
                assertThat(apiKey.getType()).isEqualTo(PlatformType.AUTOMATION);
                assertThat(apiKey.getUserId()).isEqualTo(ownerUser.getId());
                assertThat(apiKey.getSecretKey()).isNotBlank();
            });
    }

    @Test
    void testOnlyTheTenantAdminMayListPlatformKeys() {
        ApiKey platformApiKey = saveApiKey("platform key", otherUser, null);

        saveApiKey("automation key", otherUser, PlatformType.AUTOMATION);

        assertThatThrownBy(() -> apiKeyFacade.getAdminApiKeys(ENVIRONMENT_ID))
            .isInstanceOf(AccessDeniedException.class);

        authenticateAs(OWNER_LOGIN, true);

        assertThat(apiKeyFacade.getAdminApiKeys(ENVIRONMENT_ID))
            .extracting(ApiKey::getId)
            .containsExactly(platformApiKey.getId());
    }

    @Test
    void testAConnectedUserWhoseExternalIdIsAPlatformLoginIsRefusedEveryApiKeyOperation() {
        ApiKey ownerApiKey = saveApiKey("owner key", ownerUser, PlatformType.AUTOMATION);

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new TestConnectedUserAuthentication(OWNER_LOGIN));

        assertThatThrownBy(() -> apiKeyFacade.getApiKeys(ENVIRONMENT_ID, PlatformType.AUTOMATION))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> apiKeyFacade.getApiKey(ownerApiKey.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> apiKeyFacade.update(renamed(ownerApiKey.getId(), "renamed")))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> apiKeyFacade.delete(ownerApiKey.getId()))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> apiKeyFacade.createAutomationApiKey(newApiKey("connected key")))
            .isInstanceOf(AccessDeniedException.class);

        List<ApiKey> apiKeys = apiKeyRepository.findAll();

        assertThat(apiKeys)
            .singleElement()
            .satisfies(apiKey -> {
                assertThat(apiKey.getId()).isEqualTo(ownerApiKey.getId());
                assertThat(apiKey.getName()).isEqualTo("owner key");
            });
    }

    private static void authenticateAs(String login, boolean tenantAdmin) {
        String authority = tenantAdmin ? AuthorityConstants.ADMIN : AuthorityConstants.USER;

        Authentication authentication = new UsernamePasswordAuthenticationToken(
            login, null, AuthorityUtils.createAuthorityList(authority));

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(authentication);
    }

    private static ApiKey newApiKey(String name) {
        ApiKey apiKey = new ApiKey();

        apiKey.setName(name);

        return apiKey;
    }

    private static User newUser(String login) {
        User user = new User();

        user.setActivated(true);
        user.setEmail(login + "@localhost");
        user.setLangKey("en");
        user.setLogin(login);
        user.setPassword("$2a$10$" + "a".repeat(53));
        user.setUuid(UUID.randomUUID());

        return user;
    }

    private static ApiKey renamed(long id, String name) {
        ApiKey apiKey = newApiKey(name);

        apiKey.setId(id);

        return apiKey;
    }

    private ApiKey saveApiKey(String name, User user, PlatformType type) {
        ApiKey apiKey = newApiKey(name);

        UUID secretKey = UUID.randomUUID();

        apiKey.setSecretKey(secretKey.toString());
        apiKey.setType(type);
        apiKey.setUserId(user.getId());

        return apiKeyRepository.save(apiKey);
    }

    private static final class TestConnectedUserAuthentication extends AbstractAuthenticationToken
        implements ConnectedUserAuthentication {

        private final String externalUserId;

        private TestConnectedUserAuthentication(String externalUserId) {
            super(AuthorityUtils.NO_AUTHORITIES);

            this.externalUserId = externalUserId;

            setAuthenticated(true);
        }

        @Override
        public long connectedUserId() {
            return 1L;
        }

        @Override
        public long environmentId() {
            return ENVIRONMENT_ID;
        }

        @Override
        public String externalUserId() {
            return externalUserId;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return externalUserId;
        }
    }
}
