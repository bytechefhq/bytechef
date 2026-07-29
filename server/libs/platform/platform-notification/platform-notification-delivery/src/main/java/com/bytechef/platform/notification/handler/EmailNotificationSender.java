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

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.email.NotificationEmailGateway;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Delivers EMAIL-channel notifications through the {@link NotificationEmailGateway} port. The monolith and
 * configuration-app bind the port to {@code MailService} — the single email path for everything the platform sends —
 * while the distributed coordinator binds it to a remote client that proxies the send to configuration-app. Without a
 * gateway bean at all the channel warn-skips. Settings: {@code email} (recipient address, required).
 *
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class EmailNotificationSender implements NotificationSender<EmailNotificationHandler> {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    private final ObjectProvider<NotificationEmailGateway> notificationEmailGatewayObjectProvider;

    @SuppressFBWarnings("EI2")
    public EmailNotificationSender(ObjectProvider<NotificationEmailGateway> notificationEmailGatewayObjectProvider) {
        this.notificationEmailGatewayObjectProvider = notificationEmailGatewayObjectProvider;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.EMAIL;
    }

    @Override
    public void send(
        Notification notification, EmailNotificationHandler emailNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        Map<String, Object> settings = notification.getSettings();

        String email = (String) settings.get("email");

        if (email == null || email.isBlank()) {
            log.warn("Notification {} has no email address configured; skipping delivery", notification.getId());

            return;
        }

        NotificationEmailGateway notificationEmailGateway = notificationEmailGatewayObjectProvider.getIfAvailable();

        if (notificationEmailGateway == null) {
            log.warn(
                "No email gateway is available in this deployment; skipping email delivery for notification {}",
                notification.getId());

            return;
        }

        notificationEmailGateway.sendEmail(
            email, emailNotificationHandler.getSubject(notificationHandlerContext),
            emailNotificationHandler.getContent(notificationHandlerContext),
            emailNotificationHandler.isHtml());
    }
}
