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

import static org.assertj.core.api.Assertions.assertThatCode;
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
 * Pins the EMAIL channel's gateway-port semantics: a bound gateway receives the handler-rendered subject/content, a
 * deployment without a gateway bean warn-skips instead of failing the fan-out, and a notification without a recipient
 * address never reaches the gateway.
 *
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
    public void testMissingGatewaySkipsWithoutFailing() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(objectProviderOf(null));

        assertThatCode(
            () -> emailNotificationSender.send(
                notification(Map.of("email", "ops@example.com")), handler(), contextStub()))
                    .doesNotThrowAnyException();
    }

    @Test
    public void testMissingRecipientSkipsGateway() {
        EmailNotificationSender emailNotificationSender = new EmailNotificationSender(
            objectProviderOf(notificationEmailGateway));

        emailNotificationSender.send(notification(Map.of()), handler(), contextStub());

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
