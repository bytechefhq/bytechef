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

import com.bytechef.commons.util.RandomUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.constant.UserConstants;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.UserErrorType;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class UserInvitationServiceImpl implements UserInvitationService {

    private final AuthorityService authorityService;
    private final MailService mailService;
    private final TenantService tenantService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public UserInvitationServiceImpl(
        AuthorityService authorityService, MailService mailService, TenantService tenantService,
        UserService userService) {

        this.authorityService = authorityService;
        this.mailService = mailService;
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @Override
    public User inviteUser(String email, String role) {
        Authority authority = authorityService.getAuthorities()
            .stream()
            .filter(curAuthority -> Objects.equals(curAuthority.getName(), role))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException("Invalid role: " + role, UserErrorType.INVALID_ROLE));

        // Sign-in resolves the tenant by email and takes the first match, so a second account for an address another
        // tenant already holds could never be signed in to.
        if (tenantService.isMultiTenantEnabled() && tenantService.tenantIdsByUserEmailExist(email)) {
            throw new EmailAlreadyUsedException();
        }

        // The claim link is sent after commit, where a failure can no longer undo the account. Without a mail server
        // it would never be sent, so refuse before creating an account nobody can sign in to.
        if (!mailService.isMailConfigured()) {
            throw new ConfigurationException(
                "No mail server is configured, so no claim link can be mailed to '" + email + "'",
                UserErrorType.INVITATION_MAIL_NOT_SENT);
        }

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin(StringUtils.substringBefore(email, "@"));
        userDTO.setEmail(email);
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);

        // The stored password is random and disclosed to nobody -- not the invitee, not the administrator who sent the
        // invitation. It exists only because the column is non-null; the invitee sets a real one through the claim
        // link mailed below.
        User user = userService.registerUser(userDTO, RandomUtils.generatePassword());

        user.setAuthorities(Set.of(authority));

        // registerUser leaves the account non-activated for the self-registration flow, where activation proves the
        // recipient owns the mailbox. An invitation proves the same thing by different means: only the mailbox owner
        // receives the reset key, and without it the random password above makes the account unusable. Activating here
        // is also what lets requestPasswordReset issue that key at all -- it filters on isActivated.
        user.setActivated(true);

        userService.save(user);

        // orElseThrow, never ifPresent: the invitation's whole contract is that the recipient receives a claim link,
        // and the account is unusable without one -- the stored password is random and disclosed to nobody. Skipping
        // the mail quietly reported success for an account nobody can ever sign in to.
        User resetKeyUser = userService.requestPasswordReset(email)
            .orElseThrow(() -> new ConfigurationException(
                "No password reset key could be issued for '" + email + "', so no claim link can be mailed",
                UserErrorType.INVITATION_MAIL_NOT_SENT));

        sendAfterCommit(() -> mailService.sendCreationEmail(resetKeyUser));

        return user;
    }

    @Override
    public void notifyAddedToWorkspace(User user, String workspaceName) {
        sendAfterCommit(() -> mailService.sendWorkspaceMembershipEmail(user, workspaceName));
    }

    /**
     * Hands the mail to {@link MailService} only once the surrounding transaction has committed.
     *
     * <p>
     * {@code MailService} is {@code @Async}, so a mail dispatched inline leaves for the recipient's mailbox while the
     * transaction that provisioned the account is still open. Any later rollback -- a rejected role, an unknown
     * workspace, a constraint violation -- then takes the user row and its {@code reset_key} with it and leaves the
     * recipient holding a {@code password-reset/finish?key=} link that resolves to nothing. Deferring to
     * {@code afterCommit} makes the mail conditional on the provisioning it announces.
     *
     * <p>
     * With no transaction in progress there is nothing to wait for, so the mail goes immediately; that is the path the
     * unit tests take.
     */
    private static void sendAfterCommit(Runnable mailDispatch) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mailDispatch.run();

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                mailDispatch.run();
            }
        });
    }
}
