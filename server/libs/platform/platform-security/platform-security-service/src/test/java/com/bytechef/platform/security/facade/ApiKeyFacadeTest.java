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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ApiKey;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.ApiKeyRevoker;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.access.AccessDeniedException;

/**
 * An API key authenticates as its owner, so these are the tests that keep one person's programmatic identity out of
 * another's hands. Everything here is about who may see and touch a key, never about what a key can do.
 *
 * @author Ivica Cardic
 */
class ApiKeyFacadeTest {

    private static final long OWNER_USER_ID = 10L;
    private static final long OTHER_USER_ID = 20L;

    private ApiKeyFacade apiKeyFacade;
    private ApiKeyService apiKeyService;
    private MockedStatic<SecurityUtils> securityUtils;
    private UserService userService;

    @BeforeEach
    void setUp() {
        apiKeyService = mock(ApiKeyService.class);
        userService = mock(UserService.class);

        apiKeyFacade = new ApiKeyFacadeImpl(apiKeyService, userService);

        securityUtils = mockStatic(SecurityUtils.class);

        authenticateAs(OWNER_USER_ID, false);
    }

    @AfterEach
    void tearDown() {
        securityUtils.close();
    }

    @Test
    void testGetApiKeysReturnsOnlyTheCallerOwnKeys() {
        when(apiKeyService.getApiKeys(1L, PlatformType.AUTOMATION))
            .thenReturn(List.of(apiKey(1L, OWNER_USER_ID), apiKey(2L, OTHER_USER_ID), apiKey(3L, null)));

        // The repository finders scope by environment and type and never by owner, so without this filter the list is
        // every key in the environment -- an inventory of who holds machine access, handed to anyone who asks.
        assertThat(apiKeyFacade.getApiKeys(1L, PlatformType.AUTOMATION))
            .extracting(ApiKey::getId)
            .containsExactly(1L);
    }

    @Test
    void testGetApiKeysReturnsEveryKeyForTheTenantAdmin() {
        authenticateAs(OWNER_USER_ID, true);

        when(apiKeyService.getApiKeys(1L, PlatformType.AUTOMATION))
            .thenReturn(List.of(apiKey(1L, OWNER_USER_ID), apiKey(2L, OTHER_USER_ID)));

        assertThat(apiKeyFacade.getApiKeys(1L, PlatformType.AUTOMATION))
            .extracting(ApiKey::getId)
            .containsExactly(1L, 2L);
    }

    @Test
    void testGetApiKeyAnswersNotFoundForSomebodyElseKey() {
        when(apiKeyService.getApiKey(2L)).thenReturn(apiKey(2L, OTHER_USER_ID));

        // Not "forbidden": ids are sequential, and the difference between the two answers tells the caller which ids
        // exist and therefore who holds machine access. The wording matches a genuinely missing key exactly.
        assertThatThrownBy(() -> apiKeyFacade.getApiKey(2L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Api key not found for id: 2");
    }

    @Test
    void testGetApiKeyReturnsTheCallerOwnKey() {
        ApiKey ownKey = apiKey(1L, OWNER_USER_ID);

        when(apiKeyService.getApiKey(1L)).thenReturn(ownKey);

        assertThat(apiKeyFacade.getApiKey(1L)).isSameAs(ownKey);
    }

    @Test
    void testTenantAdminReadsAnyKey() {
        authenticateAs(OWNER_USER_ID, true);

        ApiKey otherKey = apiKey(2L, OTHER_USER_ID);

        when(apiKeyService.getApiKey(2L)).thenReturn(otherKey);

        assertThat(apiKeyFacade.getApiKey(2L)).isSameAs(otherKey);
    }

    @Test
    void testDeleteRefusesSomebodyElseKeyWithoutTouchingIt() {
        when(apiKeyService.getApiKey(2L)).thenReturn(apiKey(2L, OTHER_USER_ID));

        assertThatThrownBy(() -> apiKeyFacade.delete(2L)).isInstanceOf(IllegalArgumentException.class);

        // The ownership check has to precede the write, not merely accompany it: a delete that happens and then throws
        // has already destroyed somebody's credential.
        verify(apiKeyService, never()).delete(anyLong());
    }

    @Test
    void testUpdateRefusesSomebodyElseKey() {
        when(apiKeyService.getApiKey(2L)).thenReturn(apiKey(2L, OTHER_USER_ID));

        ApiKey renamed = apiKey(2L, OTHER_USER_ID);

        assertThatThrownBy(() -> apiKeyFacade.update(renamed)).isInstanceOf(IllegalArgumentException.class);

        verify(apiKeyService, never()).update(renamed);
    }

    @Test
    void testAnUnownedKeyBelongsToNobodyButTheTenantAdmin() {
        // A row whose owner cannot be read is not everybody's: it is the tenant admin's alone.
        when(apiKeyService.getApiKey(3L)).thenReturn(apiKey(3L, null));

        assertThatThrownBy(() -> apiKeyFacade.getApiKey(3L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testAnUnauthenticatedCallerOwnsNothing() {
        securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.empty());

        when(apiKeyService.getApiKey(1L)).thenReturn(apiKey(1L, OWNER_USER_ID));

        assertThatThrownBy(() -> apiKeyFacade.getApiKey(1L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testOnlyTheTenantAdminMayCreateAPlatformKey() {
        // type == null mints a platform key -- what the Admin API Keys page issues. The GraphQL argument is nullable,
        // so without this any authenticated caller could issue one for themselves.
        assertThatThrownBy(() -> apiKeyFacade.create(apiKey(0L, null), null))
            .isInstanceOf(AccessDeniedException.class);

        authenticateAs(OWNER_USER_ID, true);

        apiKeyFacade.create(apiKey(0L, null), null);

        verify(apiKeyService).create(any(ApiKey.class));
    }

    @Test
    void testAMemberMayStillCreateAnOrdinaryKey() {
        apiKeyFacade.create(apiKey(0L, null), PlatformType.AUTOMATION);

        verify(apiKeyService).create(any(ApiKey.class));
    }

    @Test
    void testOnlyTheTenantAdminMayListPlatformKeys() {
        assertThatThrownBy(() -> apiKeyFacade.getAdminApiKeys(1L)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testRevokeAllDeletesEveryKeyTheDepartingUserOwns() {
        when(apiKeyService.getUserApiKeys(OTHER_USER_ID)).thenReturn(List.of(apiKey(1L, OTHER_USER_ID),
            apiKey(2L, OTHER_USER_ID)));

        ((ApiKeyRevoker) apiKeyFacade).revokeAll(OTHER_USER_ID);

        // Not owner-filtered, and it must not be: the keys belong to the account being deleted, never to the caller.
        // Key by key through the service, because that is what publishes API_KEY_DELETED.
        verify(apiKeyService).delete(1L);
        verify(apiKeyService).delete(2L);
    }

    @Test
    void testRevokeAllDoesNothingForAUserWithNoKeys() {
        when(apiKeyService.getUserApiKeys(OTHER_USER_ID)).thenReturn(List.of());

        ((ApiKeyRevoker) apiKeyFacade).revokeAll(OTHER_USER_ID);

        verify(apiKeyService, never()).delete(anyLong());
    }

    private void authenticateAs(long userId, boolean tenantAdmin) {
        User user = new User();

        user.setId(userId);
        user.setLogin("user" + userId);

        securityUtils.when(SecurityUtils::fetchCurrentUserLogin)
            .thenReturn(Optional.of(user.getLogin()));
        securityUtils.when(() -> SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN))
            .thenReturn(tenantAdmin);

        when(userService.fetchUserByLogin(user.getLogin())).thenReturn(Optional.of(user));
        when(userService.getCurrentUser()).thenReturn(user);
    }

    private static ApiKey apiKey(long id, Long userId) {
        ApiKey apiKey = new ApiKey();

        apiKey.setId(id);

        if (userId != null) {
            apiKey.setUserId(userId);
        }

        return apiKey;
    }
}
