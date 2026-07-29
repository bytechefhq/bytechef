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

import com.bytechef.platform.mail.MailService;
import org.springframework.stereotype.Component;

/**
 * Binds the {@link NotificationEmailGateway} port to {@link MailService} — the single email path for everything the
 * platform sends. {@code MailService.sendEmail} is already {@code @Async} and warn-skips when no mail host is
 * configured, so no extra async or error plumbing is needed here.
 *
 * @author Ivica Cardic
 */
@Component
public class MailServiceNotificationEmailGateway implements NotificationEmailGateway {

    private final MailService mailService;

    public MailServiceNotificationEmailGateway(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public void sendEmail(String to, String subject, String content, boolean html) {
        mailService.sendEmail(to, subject, content, false, html);
    }
}
