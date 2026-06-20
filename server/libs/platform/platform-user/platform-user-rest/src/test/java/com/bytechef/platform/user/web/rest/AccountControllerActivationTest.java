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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.PersistentTokenService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.web.rest.webhook.SignUpWebhook;
import com.bytechef.tenant.service.TenantService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AccountControllerActivationTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private AuthorityService authorityService;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersistentTokenService persistentTokenService;

    @Mock
    private SignUpWebhook signUpWebhook;

    @Mock
    private TenantService tenantService;

    @Mock
    private UserService userService;

    private AccountController accountController;

    @BeforeEach
    void setUp() {
        accountController = new AccountController(
            applicationProperties, authorityService, mailService, passwordEncoder, persistentTokenService,
            signUpWebhook, tenantService, userService);
    }

    @Test
    void testUnknownEmailDoesNotThrowAndSendsNoMail() {
        when(userService.fetchUserByEmail("ghost@localhost.com")).thenReturn(Optional.empty());

        accountController.sendActivationEmail("ghost@localhost.com");

        verify(mailService, never()).sendActivationEmail(any());
    }

    @Test
    void testKnownEmailSendsMail() {
        User user = new User();

        when(userService.fetchUserByEmail("user@localhost.com")).thenReturn(Optional.of(user));

        accountController.sendActivationEmail("user@localhost.com");

        verify(mailService).sendActivationEmail(user);
    }
}
