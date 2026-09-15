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

package com.bytechef.platform.user.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.ApiKeyRevoker;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.service.TenantService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class UserManagementFacadeTest {

    private static final long USER_ID = 7L;

    private ApiKeyRevoker apiKeyRevoker;
    private UserManagementFacade userManagementFacade;
    private UserService userService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        apiKeyRevoker = mock(ApiKeyRevoker.class);
        userService = mock(UserService.class);

        ObjectProvider<ApiKeyRevoker> apiKeyRevokerProvider = mock(ObjectProvider.class);

        when(apiKeyRevokerProvider.getIfAvailable()).thenReturn(apiKeyRevoker);

        userManagementFacade = new UserManagementFacadeImpl(
            apiKeyRevokerProvider, mock(AuthorityService.class), mock(TenantService.class),
            mock(UserInvitationService.class), userService, mock(ObjectProvider.class));
    }

    @Test
    void testDeleteUserRevokesTheApiKeysBeforeRemovingTheAccount() {
        User user = user();

        when(userService.fetchUserByLogin("departing")).thenReturn(Optional.of(user));

        userManagementFacade.deleteUser("departing");

        // Order is not incidental: api_key.user_id is NOT NULL under fk_api_key_user, so removing the account first
        // fails on the constraint rather than orphaning the key. A key also authenticates as its owner, so one left
        // behind is a working credential for an account that no longer exists.
        InOrder inOrder = Mockito.inOrder(apiKeyRevoker, userService);

        inOrder.verify(apiKeyRevoker)
            .revokeAll(USER_ID);
        inOrder.verify(userService)
            .delete("departing");
    }

    @Test
    void testDeleteUserOfAnUnknownLoginRevokesNothing() {
        when(userService.fetchUserByLogin("ghost")).thenReturn(Optional.empty());

        userManagementFacade.deleteUser("ghost");

        verify(apiKeyRevoker, never()).revokeAll(USER_ID);
        verify(userService).delete("ghost");
    }

    private static User user() {
        User user = new User();

        user.setId(USER_ID);
        user.setLogin("departing");

        return user;
    }
}
