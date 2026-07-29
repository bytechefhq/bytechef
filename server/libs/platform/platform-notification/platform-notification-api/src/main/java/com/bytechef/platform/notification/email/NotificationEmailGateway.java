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

/**
 * Port through which the EMAIL notification channel reaches an actual mail transport. The monolith (and
 * configuration-app) binds it to {@code MailService}; apps without a mail stack — the distributed coordinator — bind it
 * to a remote client that proxies the send to configuration-app, so SMTP credentials and the templating dependency stay
 * in one place. No bean at all means the EMAIL channel warn-skips.
 *
 * @author Ivica Cardic
 */
public interface NotificationEmailGateway {

    void sendEmail(String to, String subject, String content, boolean html);
}
