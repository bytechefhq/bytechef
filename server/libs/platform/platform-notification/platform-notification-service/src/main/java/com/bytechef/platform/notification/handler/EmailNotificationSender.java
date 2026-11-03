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

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.notification.domain.Notification;
import java.util.concurrent.CompletableFuture;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class EmailNotificationSender implements NotificationSender<EmailNotificationHandler> {

    private final MailService mailService;

    public EmailNotificationSender(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public Notification.Type getType() {
        return Notification.Type.EMAIL;
    }

    @Override
    public CompletableFuture<Void> send(
        Notification notification, EmailNotificationHandler emailNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        String email = (String) notification.getSetting("email");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                "Notification " + notification.getId() + " has no email address configured");
        }

        mailService.sendEmail(
            email, emailNotificationHandler.getSubject(notificationHandlerContext),
            emailNotificationHandler.getContent(notificationHandlerContext), false,
            emailNotificationHandler.isHtml());

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public NotificationTestResult sendTest(
        Notification notification, EmailNotificationHandler emailNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        String email = (String) notification.getSetting("email");

        if (email == null || email.isBlank()) {
            return NotificationTestResult.failed("Email address is required");
        }

        try {
            mailService.sendEmailNow(
                email, emailNotificationHandler.getSubject(notificationHandlerContext),
                emailNotificationHandler.getContent(notificationHandlerContext), emailNotificationHandler.isHtml());
        } catch (MailException | IllegalStateException exception) {
            String exceptionName = exception.getClass()
                .getSimpleName();

            return NotificationTestResult.failed(exceptionName + ": " + exception.getMessage());
        } catch (RuntimeException exception) {
            String exceptionName = exception.getClass()
                .getSimpleName();

            return NotificationTestResult.failed("Test send failed (" + exceptionName + ")");
        }

        return NotificationTestResult.sent(false);
    }
}
