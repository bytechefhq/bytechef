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

package com.bytechef.platform.notification.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.mail.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

/**
 * @author Ivica Cardic
 */
class MailServiceNotificationEmailGatewayTest {

    private final MailService mailService = mock(MailService.class);
    private final MailServiceNotificationEmailGateway mailServiceNotificationEmailGateway =
        new MailServiceNotificationEmailGateway(mailService);

    @Test
    void testSendEmailSendsOnTheCallingThread() {
        mailServiceNotificationEmailGateway.sendEmail("ops@example.com", "subject", "content", true);

        verify(mailService).sendEmailOrThrow("ops@example.com", "subject", "content", false, true);
    }

    @Test
    void testSendEmailPropagatesMailFailure() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailService)
            .sendEmailOrThrow("ops@example.com", "subject", "content", false, true);

        assertThatThrownBy(
            () -> mailServiceNotificationEmailGateway.sendEmail("ops@example.com", "subject", "content", true))
                .isInstanceOf(MailSendException.class)
                .hasMessage("SMTP unavailable");
    }
}
