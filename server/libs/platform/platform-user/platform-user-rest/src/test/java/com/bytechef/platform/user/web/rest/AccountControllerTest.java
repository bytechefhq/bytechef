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

package com.bytechef.platform.user.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.exception.InvalidEmailException;
import com.bytechef.platform.user.exception.UserErrorType;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.web.rest.vm.ManagedUserVM;
import com.bytechef.platform.user.web.rest.webhook.SignUpWebhook;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class AccountControllerTest {

    private static final String REJECTED_EMAIL = "john.doe@rejected.com";

    private AccountController accountController;
    private final MailService mailService = mock(MailService.class);
    private final SignUpWebhook signUpWebhook = mock(SignUpWebhook.class);
    private final UserService userService = mock(UserService.class);

    @BeforeEach
    void beforeEach() {
        accountController = new AccountController(
            new ApplicationProperties(), mock(AuthorityService.class), mailService, mock(PasswordEncoder.class),
            mock(PersistentTokenService.class), signUpWebhook, mock(TenantService.class), userService);
    }

    @Test
    void testRegisterAccountWithRejectedEmailDomain() {
        ManagedUserVM managedUserVM = new ManagedUserVM();

        managedUserVM.setEmail(REJECTED_EMAIL);
        managedUserVM.setLogin(REJECTED_EMAIL);
        managedUserVM.setPassword("password");

        when(signUpWebhook.isEmailDomainValid(REJECTED_EMAIL)).thenReturn(false);

        assertThatThrownBy(() -> accountController.registerAccount(managedUserVM))
            .isInstanceOf(InvalidEmailException.class)
            .satisfies(exception -> assertThat(((InvalidEmailException) exception).getErrorKey())
                .isEqualTo(UserErrorType.INVALID_EMAIL.getErrorKey()));

        verify(userService, never()).registerUser(any(), anyString());
        verify(mailService, never()).sendActivationEmail(any());
    }
}
