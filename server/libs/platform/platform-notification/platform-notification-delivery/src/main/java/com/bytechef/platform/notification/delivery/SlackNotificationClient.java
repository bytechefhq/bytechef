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

package com.bytechef.platform.notification.delivery;

import com.bytechef.commons.util.JsonUtils;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Slack incoming-webhook transport. Callers supply the final message text (Slack mrkdwn allowed); this client owns the
 * {@code {"text": ...}} payload shape and delivers through {@link WebhookNotificationClient}, inheriting its SSRF
 * validation and error mapping. Slack incoming webhooks are capability URLs, so deliveries are unsigned.
 *
 * @author Ivica Cardic
 */
@Component
public class SlackNotificationClient {

    private final WebhookNotificationClient webhookNotificationClient;

    public SlackNotificationClient(WebhookNotificationClient webhookNotificationClient) {
        this.webhookNotificationClient = webhookNotificationClient;
    }

    /**
     * Posts the text to a Slack incoming-webhook URL. Throws {@link WebhookDeliveryException} on transport failure or a
     * non-2xx response and {@link IllegalArgumentException} when the URL fails SSRF validation.
     */
    public void send(String webhookUrl, String text) {
        String payload = JsonUtils.write(Map.of("text", text));

        webhookNotificationClient.deliver(WebhookDeliveryRequest.of(webhookUrl, "slack.message", payload));
    }
}
