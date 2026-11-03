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

package com.bytechef.platform.notification.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.notification.domain.Notification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class NotificationSecretSettingsTest {

    @Test
    void testApplyMovesSecretKeysOutOfPlainSettings() {
        Notification target = new Notification();

        Map<String, Object> incoming = new HashMap<>();

        incoming.put("webhook", "https://example.com/hook");
        incoming.put("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X");
        incoming.put("webhookHeaders", List.of(Map.of("name", "Authorization", "value", "token-1")));

        NotificationSecretSettings.apply(target, incoming, null);

        assertThat(target.getSettings()).containsOnlyKeys("webhook");
        assertThat(target.getSecretSettings())
            .containsEntry("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X")
            .containsKey("webhookHeaders");
    }

    @Test
    void testApplyKeepsStoredHeaderValueWhenMasked() {
        Notification stored = storedWithHeaders(List.of(Map.of("name", "Authorization", "value", "token-1")));
        Notification target = new Notification();

        NotificationSecretSettings.apply(
            target,
            Map.of(
                "webhookHeaders",
                List.of(
                    Map.of("name", "authorization", "value", "********"), Map.of("name", "X-Other", "value", "v"))),
            stored);

        assertThat(headers(target)).containsExactly(
            Map.of("name", "authorization", "value", "token-1"), Map.of("name", "X-Other", "value", "v"));
    }

    @Test
    void testApplyKeepsStoredHeaderValueWhenValueOmitted() {
        Notification stored = storedWithHeaders(List.of(Map.of("name", "Authorization", "value", "token-1")));
        Notification target = new Notification();

        NotificationSecretSettings.apply(
            target, Map.of("webhookHeaders", List.of(Map.of("name", "Authorization"))), stored);

        assertThat(headers(target)).containsExactly(Map.of("name", "Authorization", "value", "token-1"));
    }

    @Test
    void testApplyDropsHeaderRemovedByClient() {
        Notification stored = storedWithHeaders(
            List.of(Map.of("name", "Authorization", "value", "token-1"), Map.of("name", "X-Old", "value", "old")));
        Notification target = new Notification();

        NotificationSecretSettings.apply(
            target, Map.of("webhookHeaders", List.of(Map.of("name", "Authorization", "value", "********"))), stored);

        assertThat(headers(target)).containsExactly(Map.of("name", "Authorization", "value", "token-1"));
    }

    @Test
    void testApplyKeepsStoredSlackUrlWhenMasked() {
        Notification stored = new Notification();

        stored.setSettings(Map.of());
        stored.setSecretSettings(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X"));

        Notification target = new Notification();

        NotificationSecretSettings.apply(target, Map.of("slackWebhookUrl", "********"), stored);

        assertThat(target.getSecretSettings()).containsEntry(
            "slackWebhookUrl", "https://hooks.slack.com/services/T/B/X");
        assertThat(target.getSettings()).isEmpty();
    }

    @Test
    void testApplyKeepsStoredSigningSecret() {
        Notification stored = new Notification();

        stored.setSettings(Map.of());
        stored.setSecretSettings(Map.of("webhookSecret", "whsec_abc"));

        Notification target = new Notification();

        NotificationSecretSettings.apply(target, Map.of("webhook", "https://example.com/hook"), stored);

        assertThat(target.getSecretSettings()).containsEntry("webhookSecret", "whsec_abc");
    }

    @Test
    void testMaskedNeverContainsSecrets() {
        Notification notification = new Notification();

        notification.setSettings(Map.of("webhook", "https://example.com/hook"));
        notification.setSecretSettings(
            Map.of(
                "slackWebhookUrl", "https://hooks.slack.com/services/T/B/X", "webhookSecret", "whsec_abc",
                "webhookHeaders", List.of(Map.of("name", "Authorization", "value", "token-1"))));

        Map<String, Object> masked = NotificationSecretSettings.masked(notification);

        assertThat(masked).doesNotContainKey("webhookSecret");
        assertThat(masked).containsEntry("slackWebhookUrl", "********")
            .containsEntry("webhook", "https://example.com/hook")
            .containsEntry("webhookHeaders", List.of(Map.of("name", "Authorization", "value", "********")));
        assertThat(masked.toString()).doesNotContain("token-1", "whsec_abc", "hooks.slack.com");
    }

    @Test
    void testMaskedCoversLegacySlackUrlInPlainSettings() {
        Notification notification = new Notification();

        notification.setSettings(Map.of("slackWebhookUrl", "https://hooks.slack.com/services/T/B/X"));

        Map<String, Object> masked = NotificationSecretSettings.masked(notification);

        assertThat(masked).containsEntry("slackWebhookUrl", "********");
    }

    private static Notification storedWithHeaders(List<Map<String, Object>> headers) {
        Notification stored = new Notification();

        stored.setSettings(Map.of());
        stored.setSecretSettings(Map.of("webhookHeaders", headers));

        return stored;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> headers(Notification notification) {
        return (List<Map<String, Object>>) notification.getSecretSettings()
            .get("webhookHeaders");
    }
}
