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

package com.bytechef.platform.configuration.notification;

import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.mail.MailService;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
public class EmailNotificationSender implements NotificationSender<EmailNotificationHandler> {

    private final MailService mailService;

    public EmailNotificationSender(MailService mailService) {
        this.mailService = mailService;
    }

    public Notification.Type getType() {
        return Notification.Type.EMAIL;
    }

    @Override
    public void send(
        Notification notification, EmailNotificationHandler emailNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        Map<String, Object> settings = notification.getSettings();

        mailService.sendEmail(
            (String) settings.get("email"), emailNotificationHandler.getSubject(notificationHandlerContext),
            emailNotificationHandler.getContent(notificationHandlerContext), false, emailNotificationHandler.isHtml());
    }
}
