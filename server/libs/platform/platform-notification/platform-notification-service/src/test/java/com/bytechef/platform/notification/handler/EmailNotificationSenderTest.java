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

package com.bytechef.platform.notification.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.domain.Notification;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

class EmailNotificationSenderTest {

    private static final String CONTENT = "Job completed";
    private static final String EMAIL = "john.doe@example.com";
    private static final String SUBJECT = "Job status";

    private final EmailNotificationHandler emailNotificationHandler = mock(EmailNotificationHandler.class);
    private final MailService mailService = mock(MailService.class);
    private final NotificationHandlerContext notificationHandlerContext = mock(NotificationHandlerContext.class);
    private final EmailNotificationSender emailNotificationSender = new EmailNotificationSender(mailService);
    private final Notification notification = new Notification();

    @BeforeEach
    void beforeEach() {
        notification.setSettings(Map.of("email", EMAIL));

        when(emailNotificationHandler.getContent(notificationHandlerContext)).thenReturn(CONTENT);
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn(SUBJECT);
        when(emailNotificationHandler.isHtml()).thenReturn(true);
    }

    @Test
    void testSend() {
        emailNotificationSender.send(notification, emailNotificationHandler, notificationHandlerContext);

        verify(mailService).sendEmailOrThrow(EMAIL, SUBJECT, CONTENT, false, true);
    }

    @Test
    void testSendPropagatesMailFailure() {
        doThrow(new MailSendException("SMTP unavailable")).when(mailService)
            .sendEmailOrThrow(EMAIL, SUBJECT, CONTENT, false, true);

        assertThatThrownBy(
            () -> emailNotificationSender.send(notification, emailNotificationHandler, notificationHandlerContext))
                .isInstanceOf(MailSendException.class)
                .hasMessage("SMTP unavailable");
    }
}
