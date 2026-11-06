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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 * @author Ivica Cardic
 */
@Component
public class EmailNotificationSender implements NotificationSender<EmailNotificationHandler> {

    private static final int MAX_ERROR_LENGTH = 200;
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

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
    public CompletableFuture<Void> send(
        Notification notification, EmailNotificationHandler emailNotificationHandler,
        NotificationHandlerContext notificationHandlerContext) {

        String email = (String) notification.getSetting("email");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                "Notification " + notification.getId() + " has no email address configured");
        }

        NotificationEmailGateway notificationEmailGateway = notificationEmailGatewayObjectProvider.getIfAvailable();

        if (notificationEmailGateway == null) {
            throw new IllegalStateException(
                "No email gateway is available in this deployment to deliver notification " + notification.getId());
        }

        notificationEmailGateway.sendEmail(
            email, emailNotificationHandler.getSubject(notificationHandlerContext),
            emailNotificationHandler.getContent(notificationHandlerContext),
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

        NotificationEmailGateway notificationEmailGateway = notificationEmailGatewayObjectProvider.getIfAvailable();

        if (notificationEmailGateway == null) {
            return NotificationTestResult.failed("No email gateway is available in this deployment");
        }

        try {
            notificationEmailGateway.sendEmailNow(
                email, emailNotificationHandler.getSubject(notificationHandlerContext),
                emailNotificationHandler.getContent(notificationHandlerContext), emailNotificationHandler.isHtml());
        } catch (MailException | IllegalStateException exception) {
            String exceptionName = exception.getClass()
                .getSimpleName();

            return NotificationTestResult.failed(abbreviate(exceptionName + ": " + exception.getMessage()));
        } catch (RuntimeException exception) {
            String exceptionName = exception.getClass()
                .getSimpleName();

            return NotificationTestResult.failed("Test send failed (" + exceptionName + ")");
        }

        return NotificationTestResult.sent(false);
    }

    private static String abbreviate(String text) {
        String singleLineText = WHITESPACE_PATTERN.matcher(text)
            .replaceAll(" ");

        singleLineText = singleLineText.strip();

        if (singleLineText.length() <= MAX_ERROR_LENGTH) {
            return singleLineText;
        }

        int end = MAX_ERROR_LENGTH;

        if (Character.isHighSurrogate(singleLineText.charAt(end - 1))) {
            end--;
        }

        return singleLineText.substring(0, end) + "…";
    }
}
