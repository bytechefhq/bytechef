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
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.UserErrorType;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        // The afterCommit tests start a synchronization by hand; leaking it would change what later tests observe.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clear();
        }
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

    @Test
    void testInviteUserFailsLoudlyWhenNoClaimLinkCanBeIssued() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(userService.registerUser(any(), anyString())).thenReturn(new User());
        when(userService.requestPasswordReset(EMAIL)).thenReturn(Optional.empty());

        // Silently skipping the mail reported a successful invitation for an account whose only credential is a random
        // password nobody holds -- the invitee could never sign in and nobody was told.
        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("no claim link can be mailed");

        verify(mailService, never()).sendCreationEmail(any());
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
    void testInviteUserHoldsTheClaimLinkUntilTheTransactionCommits() {
        User user = givenRegisterableUser();

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.inviteUser(EMAIL, ROLE);

            // MailService is @Async, so a mail handed over inside the transaction is already on its way to the
            // recipient when a later rollback removes the account and its reset_key, leaving a dead claim link.
            verify(mailService, never()).sendCreationEmail(user);

            runAfterCommitCallbacks();

            verify(mailService).sendCreationEmail(user);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testInviteUserWithholdsTheClaimLinkUntilAfterCommitRuns() {
        // Pins the precondition a rollback relies on: with a transaction open and no afterCommit callback invoked --
        // committed or not, nothing has run yet -- the mail has not gone out. This does not simulate a rollback; that
        // is testInviteUserNeverMailsTheClaimLinkWhenTheTransactionRollsBack below.
        User user = givenRegisterableUser();

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.inviteUser(EMAIL, ROLE);

            verify(mailService, never()).sendCreationEmail(user);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testInviteUserNeverMailsTheClaimLinkWhenTheTransactionRollsBack() {
        User user = givenRegisterableUser();

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.inviteUser(EMAIL, ROLE);

            // The real rollback path: Spring's transaction manager never calls afterCommit on a rolled-back
            // transaction, it calls afterCompletion(STATUS_ROLLED_BACK) instead. sendAfterCommit's synchronization
            // does not override afterCompletion, so this exercises the interface's default no-op -- which is the
            // point: a rollback must reach the invitee's mailbox in no way at all, not even through a callback the
            // sender forgot to suppress.
            runAfterCompletionRolledBack();

            verify(mailService, never()).sendCreationEmail(user);
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testNotifyAddedToWorkspaceHoldsTheMailUntilTheTransactionCommits() {
        User user = new User();

        TransactionSynchronizationManager.initSynchronization();

        try {
            userInvitationService.notifyAddedToWorkspace(user, "Engineering");

            verify(mailService, never()).sendWorkspaceMembershipEmail(user, "Engineering");

            runAfterCommitCallbacks();

            verify(mailService).sendWorkspaceMembershipEmail(user, "Engineering");
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }

    @Test
    void testMissingClaimLinkCarriesTheInvitationMailErrorType() {
        when(authorityService.getAuthorities()).thenReturn(List.of(createAuthority(ROLE)));
        when(userService.registerUser(any(), anyString())).thenReturn(new User());
        when(userService.requestPasswordReset(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userInvitationService.inviteUser(EMAIL, ROLE))
            .isInstanceOf(ConfigurationException.class)
            .extracting(thrown -> ((ConfigurationException) thrown).getErrorKey())
            .isEqualTo(UserErrorType.INVITATION_MAIL_NOT_SENT.getErrorKey());
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
