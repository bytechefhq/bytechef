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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.email.NotificationEmailGateway;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
public class EmailNotificationSenderTest {

    private final NotificationEmailGateway notificationEmailGateway = mock(NotificationEmailGateway.class);

    @Test
    public void testDelegatesToGateway() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(
            objectProviderOf(notificationEmailGateway));

        emailNotificationSender.send(
            notification(Map.of("email", "ops@example.com")), handler(), contextStub());

        verify(notificationEmailGateway).sendEmail("ops@example.com", "subject", "content", true);
    }

    @Test
    public void testSendReturnsACompletedFuture() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(
            objectProviderOf(notificationEmailGateway));

        assertThat(
            emailNotificationSender.send(notification(Map.of("email", "ops@example.com")), handler(), contextStub()))
                .isCompleted();
    }

    @Test
    public void testGatewayFailurePropagates() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(
            objectProviderOf(notificationEmailGateway));

        doThrow(new IllegalStateException("SMTP unavailable")).when(notificationEmailGateway)
            .sendEmail("ops@example.com", "subject", "content", true);

        assertThatThrownBy(
            () -> emailNotificationSender.send(
                notification(Map.of("email", "ops@example.com")), handler(), contextStub()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("SMTP unavailable");
    }

    @Test
    public void testMissingGatewayThrows() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(objectProviderOf(null));

        assertThatThrownBy(
            () -> emailNotificationSender.send(
                notification(Map.of("email", "ops@example.com")), handler(), contextStub()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No email gateway");
    }

    @Test
    public void testMissingRecipientThrowsWithoutCallingGateway() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(
            objectProviderOf(notificationEmailGateway));

        assertThatThrownBy(() -> emailNotificationSender.send(notification(Map.of()), handler(), contextStub()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("no email address");

        verifyNoInteractions(notificationEmailGateway);
    }

    @Test
    public void testBlankRecipientThrowsWithoutCallingGateway() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(
            objectProviderOf(notificationEmailGateway));

        assertThatThrownBy(
            () -> emailNotificationSender.send(notification(Map.of("email", " ")), handler(), contextStub()))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(notificationEmailGateway);
    }

    private static Notification notification(Map<String, ?> settings) {
        Notification notification = new Notification();

        notification.setSettings(settings);

        return notification;
    }

    private EmailNotificationHandler handler() {
        EmailNotificationHandler emailNotificationHandler = mock(EmailNotificationHandler.class);

        when(emailNotificationHandler.getSubject(org.mockito.ArgumentMatchers.any())).thenReturn("subject");
        when(emailNotificationHandler.getContent(org.mockito.ArgumentMatchers.any())).thenReturn("content");
        when(emailNotificationHandler.isHtml()).thenReturn(true);

        return emailNotificationHandler;
    }

    private static NotificationHandlerContext contextStub() {
        return mock(NotificationHandlerContext.class);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<NotificationEmailGateway> objectProviderOf(
        NotificationEmailGateway notificationEmailGateway) {

        ObjectProvider<NotificationEmailGateway> objectProvider = (ObjectProvider<NotificationEmailGateway>) mock(
            ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(notificationEmailGateway);

        return objectProvider;
    }
}
