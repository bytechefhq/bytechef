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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.service.TenantService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
class UserManagementFacadeImplTest {

    private TenantService tenantService;
    private UserInvitationService userInvitationService;
    private UserService userService;
    private UserManagementFacadeImpl userManagementFacade;

    @BeforeEach
    void setUp() {
        tenantService = Mockito.mock(TenantService.class);
        userInvitationService = Mockito.mock(UserInvitationService.class);
        userService = Mockito.mock(UserService.class);

        userManagementFacade = new UserManagementFacadeImpl(
            Mockito.mock(AuthorityService.class), tenantService, userInvitationService, userService);
    }

    @Test
    void testInviteUserDelegatesToTheInvitationService() {
        when(userService.fetchUserByEmail("newuser@example.com")).thenReturn(Optional.empty());

        userManagementFacade.inviteUser("newuser@example.com", "ROLE_USER");

        verify(userInvitationService).inviteUser("newuser@example.com", "ROLE_USER");
    }

    @Test
    void testInviteUserWithExistingEmailThrows() {
        User user = new User();

        user.setEmail("existing@example.com");
        user.setLogin("existing@example.com");

        when(userService.fetchUserByEmail("existing@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userManagementFacade.inviteUser("existing@example.com", "ROLE_USER"))
            .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userInvitationService, never()).inviteUser(anyString(), anyString());
    }

    @Test
    void testInviteUserWithEmailOfAnotherTenantThrows() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.tenantIdsByUserEmailExist("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userManagementFacade.inviteUser("taken@example.com", "ROLE_USER"))
            .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userInvitationService, never()).inviteUser(anyString(), anyString());
    }
}
