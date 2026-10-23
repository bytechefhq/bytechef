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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.InvalidEmailException;
import com.bytechef.platform.user.exception.LoginAlreadyUsedException;
import com.bytechef.platform.user.exception.UserErrorType;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
class UserInvitationServiceTest {

    private static final String ACTIVATION_KEY = "invitation-key";
    private static final String EMAIL = "newuser@example.com";
    private static final String ROLE = "ROLE_USER";

    private AuthorityService authorityService;
    private MailService mailService;
    private TenantService tenantService;
    private UserService userService;
    private UserInvitationServiceImpl userInvitationService;

    @BeforeEach
    void setUp() {
        authorityService = mock(AuthorityService.class);
        mailService = mock(MailService.class);
        tenantService = mock(TenantService.class);
        userService = mock(UserService.class);

        when(mailService.isMailConfigured()).thenReturn(true);

        userInvitationService = new UserInvitationServiceImpl(
            authorityService, mailService, tenantService, userService);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testInviteUserMailsTheInvitationLink() {
        User user = givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        verify(mailService).sendCreationEmail(user);
    }

    @Test
    void testInviteUserStoresAGeneratedPasswordNobodySupplied() {
        givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        verify(userService).registerUser(any(), passwordCaptor.capture());

        assertThat(passwordCaptor.getValue()).isNotBlank();
    }

    @Test
    void testInviteUserLeavesTheAccountInactiveUntilTheInviteeSetsAPassword() {
        User user = givenRegisterableUser();

        userInvitationService.inviteUser(EMAIL, ROLE);

        assertThat(user.isActivated()).isFalse();
        assertThat(user.isInvited()).isTrue();
        assertThat(user.getActivationKey()).isEqualTo(ACTIVATION_KEY);

        verify(userService, never()).requestPasswordReset(anyString());
    }

    @Test
    void testInviteUserUsesTheLowerCasedEmailAsTheLogin() {
        String email = "NewUser@Example.com";
        givenRegisterableUser();

        userInvitationService.inviteUser(email, ROLE);

        assertThat(captureRegisteredLogin()).isEqualTo(EMAIL);
    }

    @Test
    void testInviteUserRejectsALoginAnotherTenantHolds() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.tenantIdsByUserLoginExist(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(LoginAlreadyUsedException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserRejectsALoginAnAccountInThisTenantHolds() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(userService.fetchUserByLogin(EMAIL)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(LoginAlreadyUsedException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserRejectsAnEmailLongerThanTheLoginLimit() {
        String email = "firstname.middlename.lastname.department@subdomain.example.com";

        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));

        assertThatThrownBy(() -> userInvitationService.inviteUser(email, ROLE))
            .isInstanceOf(InvalidEmailException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserRejectsUnknownRoleBeforeProvisioning() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, "ROLE_NONSENSE"))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("Invalid role");

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserRefusesBeforeProvisioningWhenNoMailServerIsConfigured() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(mailService.isMailConfigured()).thenReturn(false);

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("No mail server is configured");

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserRefusesAnAddressAnotherTenantHoldsBeforeProvisioning() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.tenantIdsByUserEmailExist(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserHoldsTheInvitationLinkUntilTheTransactionCommits() {
        User user = givenRegisterableUser();

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.inviteUser(EMAIL, ROLE);

            verify(mailService, never()).sendCreationEmail(user);

            runAfterCommitCallbacks();

            verify(mailService).sendCreationEmail(user);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testInviteUserNeverMailsTheInvitationLinkWhenTheTransactionRollsBack() {
        User user = givenRegisterableUser();

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.inviteUser(EMAIL, ROLE);

            runAfterCompletionRolledBack();

            verify(mailService, never()).sendCreationEmail(user);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testInviteUserReportsAnInvitationLinkMailThatCouldNotBeSent() {
        User user = givenRegisterableUser();

        doThrow(new MailSendException("SMTP server unavailable")).when(mailService)
            .sendCreationEmail(user);

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("could not be sent")
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(UserErrorType.INVITATION_MAIL_NOT_SENT.getErrorKey());
    }

    @Test
    void testInviteUserReportsAnInvitationLinkMailThatFailedAfterCommitToTheCommittingCaller() {
        User user = givenRegisterableUser();

        doThrow(new MailSendException("SMTP server unavailable")).when(mailService)
            .sendCreationEmail(user);

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.inviteUser(EMAIL, ROLE);

            verify(mailService, never()).sendCreationEmail(user);

            assertThatThrownBy(this::runAfterCommitCallbacks)
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("could not be sent");
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    private void runAfterCommitCallbacks() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }

    private void runAfterCompletionRolledBack() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
    }

    private String captureRegisteredLogin() {
        ArgumentCaptor<AdminUserDTO> userDTOCaptor = ArgumentCaptor.forClass(AdminUserDTO.class);

        verify(userService).registerUser(userDTOCaptor.capture(), anyString());

        AdminUserDTO userDTO = userDTOCaptor.getValue();

        return userDTO.getLogin();
    }

    private User givenRegisterableUser() {
        User user = new User();

        user.setActivated(false);
        user.setActivationKey(ACTIVATION_KEY);
        user.setLogin("newuser");
        user.setEmail(EMAIL);

        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(userService.registerUser(any(), anyString())).thenReturn(user);

        return user;
    }

    private Authority createAuthority(String name) {
        Authority authority = new Authority();

        authority.setName(name);

        return authority;
    }
}
