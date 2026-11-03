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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.domain.Notification;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

/**
 * @author Ivica Cardic
 */
class EmailNotificationSenderTest {

    private final EmailNotificationHandler emailNotificationHandler = mock(EmailNotificationHandler.class);
    private final MailService mailService = mock(MailService.class);
    private final EmailNotificationSender emailNotificationSender = new EmailNotificationSender(mailService);
    private final NotificationHandlerContext notificationHandlerContext =
        new NotificationHandlerContext.Builder().build();

    @Test
    void testSendMailsHandlerContentToConfiguredAddress() {
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn("Subject");
        when(emailNotificationHandler.getContent(notificationHandlerContext)).thenReturn("Content");
        when(emailNotificationHandler.isHtml()).thenReturn(true);

        emailNotificationSender.send(
            createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
            notificationHandlerContext);

        verify(mailService).sendEmail("approver@example.com", "Subject", "Content", false, true);
    }

    @Test
    void testSendReturnsACompletedFuture() {
        assertThat(
            emailNotificationSender.send(
                createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
                notificationHandlerContext))
                    .isCompleted();
    }

    @Test
    void testSendThrowsWhenEmailMissing() {
        assertThatThrownBy(
            () -> emailNotificationSender.send(
                createNotification(Map.of()), emailNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no email address");

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void testSendThrowsWhenEmailBlank() {
        assertThatThrownBy(
            () -> emailNotificationSender.send(
                createNotification(Map.of("email", " ")), emailNotificationHandler, notificationHandlerContext))
                    .isInstanceOf(IllegalStateException.class);

        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
    }

    @Test
    void testSendTestSendsSynchronously() {
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn("Subject");
        when(emailNotificationHandler.getContent(notificationHandlerContext)).thenReturn("Content");
        when(emailNotificationHandler.isHtml()).thenReturn(true);

        NotificationTestResult notificationTestResult = emailNotificationSender.sendTest(
            createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
            notificationHandlerContext);

        verify(mailService).sendEmailNow("approver@example.com", "Subject", "Content", true);
        verify(mailService, never()).sendEmail(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());

        assertThat(notificationTestResult.success()).isTrue();
        assertThat(notificationTestResult.statusCode()).isNull();
        assertThat(notificationTestResult.signed()).isFalse();
    }

    @Test
    void testSendTestReportsMailFailure() {
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn("Subject");
        when(emailNotificationHandler.getContent(notificationHandlerContext)).thenReturn("Content");

        doThrow(new MailSendException("boom")).when(mailService)
            .sendEmailNow(anyString(), anyString(), anyString(), anyBoolean());

        NotificationTestResult notificationTestResult = emailNotificationSender.sendTest(
            createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
            notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("MailSendException: boom");
    }

    @Test
    void testSendTestReportsMailNotConfigured() {
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn("Subject");
        when(emailNotificationHandler.getContent(notificationHandlerContext)).thenReturn("Content");

        doThrow(new IllegalStateException("Mail server is not configured")).when(mailService)
            .sendEmailNow(anyString(), anyString(), anyString(), anyBoolean());

        assertThat(
            emailNotificationSender.sendTest(
                createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
                notificationHandlerContext)
                .error()).isEqualTo("IllegalStateException: Mail server is not configured");
    }

    @Test
    void testSendTestMapsUnexpectedExceptionWithoutMessage() {
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn("Subject");
        when(emailNotificationHandler.getContent(notificationHandlerContext))
            .thenThrow(new IllegalArgumentException("secret-value"));

        NotificationTestResult notificationTestResult = emailNotificationSender.sendTest(
            createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
            notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("Test send failed (IllegalArgumentException)");
        assertThat(notificationTestResult.error()).doesNotContain("secret-value");
    }

    @Test
    void testSendTestFailsWhenEmailMissing() {
        NotificationTestResult notificationTestResult = emailNotificationSender.sendTest(
            createNotification(Map.of()), emailNotificationHandler, notificationHandlerContext);

        assertThat(notificationTestResult.success()).isFalse();
        assertThat(notificationTestResult.error()).isEqualTo("Email address is required");
    }

    @Test
    void testSendTestCollapsesAndCapsErrorText() {
        when(emailNotificationHandler.getSubject(notificationHandlerContext)).thenReturn("Subject");
        when(emailNotificationHandler.getContent(notificationHandlerContext)).thenReturn("Content");

        String message = ("line one\n\tline two   " + "x".repeat(50) + "\r\n").repeat(14)
            .substring(0, 1000);

        doThrow(new MailSendException(message)).when(mailService)
            .sendEmailNow(anyString(), anyString(), anyString(), anyBoolean());

        String error = emailNotificationSender.sendTest(
            createNotification(Map.of("email", "approver@example.com")), emailNotificationHandler,
            notificationHandlerContext)
            .error();

        assertThat(message).hasSize(1000)
            .contains("\n");
        assertThat(error).hasSize(201)
            .startsWith("MailSendException: line one line two xxx")
            .endsWith("\u2026")
            .doesNotContain("\n", "\r", "\t", "  ");
    }

    private static Notification createNotification(Map<String, Object> settings) {
        Notification notification = new Notification();

        notification.setId(1L);
        notification.setType(Notification.Type.EMAIL);
        notification.setSettings(settings);

        return notification;
    }
}
