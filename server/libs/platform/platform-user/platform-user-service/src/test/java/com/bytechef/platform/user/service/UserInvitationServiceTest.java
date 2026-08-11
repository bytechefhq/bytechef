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

package com.bytechef.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Covers the shared invitation sequence: what is provisioned, what is mailed, and — most importantly — what is never
 * disclosed.
 *
 * @author Ivica Cardic
 */
class UserInvitationServiceTest {

    private static final String EMAIL = "newuser@example.com";
    private static final String ROLE = "ROLE_USER";

    private AuthorityService authorityService;
    private MailService mailService;
    private UserService userService;
    private UserInvitationServiceImpl userInvitationService;

    @BeforeEach
    void setUp() {
        authorityService = mock(AuthorityService.class);
        mailService = mock(MailService.class);
        userService = mock(UserService.class);

        userInvitationService = new UserInvitationServiceImpl(authorityService, mailService, userService);
    }

    @Test
    void testInviteUserMailsTheClaimLink() {
        User user = givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        verify(mailService).sendCreationEmail(user);
    }

    @Test
    void testInviteUserStoresAGeneratedPasswordNobodySupplied() {
        givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        // The account is usable only through the claim link. A non-blank generated value reaching registerUser is the
        // point: no caller supplies it, so no caller learns it.
        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(userService).registerUser(any(), passwordCaptor.capture());

        assertThat(passwordCaptor.getValue()).isNotBlank();
    }

    @Test
    void testInviteUserActivatesSoTheClaimLinkCanBeIssued() {
        User user = givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        // requestPasswordReset filters on isActivated, so leaving the account non-activated would mail nothing and
        // strand the invitee with a password they cannot learn.
        assertThat(user.isActivated()).isTrue();
    }

    @Test
    void testInviteUserDerivesTheLoginFromTheEmail() {
        givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        ArgumentCaptor<com.bytechef.platform.user.dto.AdminUserDTO> userDTOCaptor =
            ArgumentCaptor.forClass(com.bytechef.platform.user.dto.AdminUserDTO.class);

        verify(userService).registerUser(userDTOCaptor.capture(), anyString());

        assertThat(userDTOCaptor.getValue()
            .getLogin()).isEqualTo("newuser");
    }

    @Test
    void testInviteUserRejectsUnknownRoleBeforeProvisioning() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, "ROLE_NONSENSE"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Invalid role");

        verify(userService, never()).registerUser(any(), anyString());
    }

    private User givenRegisterableUser() {
        User user = new User();

        user.setLogin("newuser");
        user.setEmail(EMAIL);

        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(userService.registerUser(any(), anyString())).thenReturn(user);
        when(userService.requestPasswordReset(EMAIL)).thenReturn(Optional.of(user));

        return user;
    }

    private Authority createAuthority(String name) {
        Authority authority = new Authority();

        authority.setName(name);

        return authority;
    }
}
