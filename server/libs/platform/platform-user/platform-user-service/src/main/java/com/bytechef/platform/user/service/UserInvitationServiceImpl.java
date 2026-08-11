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
import com.bytechef.platform.user.exception.UserErrorType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class UserInvitationServiceImpl implements UserInvitationService {

    private final AuthorityService authorityService;
    private final MailService mailService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public UserInvitationServiceImpl(
        AuthorityService authorityService, MailService mailService, UserService userService) {

        this.authorityService = authorityService;
        this.mailService = mailService;
        this.userService = userService;
    }

    @Override
    public User inviteUser(String email, String role) {
        Authority authority = authorityService.getAuthorities()
            .stream()
            .filter(curAuthority -> Objects.equals(curAuthority.getName(), role))
            .findFirst()
            .orElseThrow(() -> new ConfigurationException("Invalid role: " + role, UserErrorType.INVALID_ROLE));

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

        userService.requestPasswordReset(email)
            .ifPresent(mailService::sendCreationEmail);

        return user;
    }

    @Override
    public void notifyAddedToWorkspace(User user, String workspaceName) {
        mailService.sendWorkspaceMembershipEmail(user, workspaceName);
    }
}
