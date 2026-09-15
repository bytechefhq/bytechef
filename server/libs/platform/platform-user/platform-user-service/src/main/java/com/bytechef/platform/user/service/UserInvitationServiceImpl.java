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
import com.bytechef.platform.user.exception.InvalidEmailException;
import com.bytechef.platform.user.exception.LoginAlreadyUsedException;
import com.bytechef.platform.user.exception.UserErrorType;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.mail.MailException;
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
        if (email.length() > UserConstants.LOGIN_MAX_LENGTH) {
            throw new InvalidEmailException(email);
        }

        Authority authority = authorityService.getAuthorities()
            .stream()
            .filter(curAuthority -> Objects.equals(curAuthority.getName(), role))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException("Invalid role: " + role, UserErrorType.INVALID_ROLE));

        if (tenantService.isMultiTenantEnabled() && tenantService.tenantIdsByUserEmailExist(email)) {
            throw new EmailAlreadyUsedException();
        }

        if (!mailService.isMailConfigured()) {
            throw new ConfigurationException(
                "No mail server is configured, so no invitation can be mailed to '" + email + "'",
                UserErrorType.INVITATION_MAIL_NOT_SENT);
        }

        String login = email.toLowerCase(Locale.ENGLISH);

        if (isLoginUsed(login)) {
            throw new LoginAlreadyUsedException();
        }

        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin(login);
        userDTO.setEmail(email);
        userDTO.setLangKey(UserConstants.DEFAULT_LANGUAGE);

        User user = userService.registerUser(userDTO, RandomUtils.generatePassword());

        user.setAuthorities(Set.of(authority));
        user.setInvited(true);

        userService.save(user);

        sendAfterCommit(() -> sendCreationEmail(user));

        return user;
    }

    @Override
    public void notifyAddedToWorkspace(User user, String workspaceName) {
        sendAfterCommit(() -> mailService.sendWorkspaceMembershipEmail(user, workspaceName));
    }

    private boolean isLoginUsed(String login) {
        if (tenantService.isMultiTenantEnabled() && tenantService.tenantIdsByUserLoginExist(login)) {
            return true;
        }

        Optional<User> existingUser = userService.fetchUserByLogin(login);

        return existingUser.isPresent();
    }

    private void sendCreationEmail(User user) {
        try {
            mailService.sendCreationEmail(user);
        } catch (MailException mailException) {
            throw new ConfigurationException(
                "The invitation for '" + user.getEmail() + "' was created, but the email with its link could not " +
                    "be sent. Delete the user and invite them again.",
                mailException, UserErrorType.INVITATION_MAIL_NOT_SENT);
        }
    }

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
