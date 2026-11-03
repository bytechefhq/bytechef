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

import com.bytechef.platform.notification.domain.Notification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * Splits notification settings into plain and secret parts, resolves masked values against the stored notification and
 * builds the masked view returned by the API.
 *
 * @author Ivica Cardic
 */
final class NotificationSecretSettings {

    static final String MASK = "********";
    static final String SIGNING_SECRET_KEY = "webhookSecret";
    static final String SLACK_WEBHOOK_URL_KEY = "slackWebhookUrl";
    static final String WEBHOOK_HEADERS_KEY = "webhookHeaders";
    static final Set<String> SECRET_KEYS = Set.of(SLACK_WEBHOOK_URL_KEY, WEBHOOK_HEADERS_KEY, SIGNING_SECRET_KEY);

    private NotificationSecretSettings() {
    }

    /**
     * Moves the secret keys of the incoming settings into the secret settings of the target. A secret sent back as the
     * mask, or omitted, keeps the stored value. The signing secret is never taken from the incoming settings.
     */
    static void apply(Notification target, Map<String, Object> incomingSettings, @Nullable Notification stored) {
        Map<String, Object> incoming = incomingSettings == null ? Map.of() : incomingSettings;
        Map<String, Object> plainSettings = new HashMap<>();
        Map<String, Object> secretSettings = new HashMap<>();

        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            if (!SECRET_KEYS.contains(entry.getKey())) {
                plainSettings.put(entry.getKey(), entry.getValue());
            }
        }

        Object storedSlackUrl = stored == null ? null : stored.getSetting(SLACK_WEBHOOK_URL_KEY);

        if (incoming.containsKey(SLACK_WEBHOOK_URL_KEY)) {
            Object slackUrl = incoming.get(SLACK_WEBHOOK_URL_KEY);

            if (MASK.equals(slackUrl)) {
                slackUrl = storedSlackUrl;
            }

            if (slackUrl != null) {
                secretSettings.put(SLACK_WEBHOOK_URL_KEY, slackUrl);
            }
        } else if (storedSlackUrl != null) {
            secretSettings.put(SLACK_WEBHOOK_URL_KEY, storedSlackUrl);
        }

        Object storedHeaders = stored == null ? null : stored.getSetting(WEBHOOK_HEADERS_KEY);

        if (incoming.containsKey(WEBHOOK_HEADERS_KEY)) {
            List<Map<String, Object>> headers = resolveHeaders(incoming.get(WEBHOOK_HEADERS_KEY), storedHeaders);

            if (!headers.isEmpty()) {
                secretSettings.put(WEBHOOK_HEADERS_KEY, headers);
            }
        } else if (storedHeaders != null) {
            secretSettings.put(WEBHOOK_HEADERS_KEY, storedHeaders);
        }

        Object storedSigningSecret = stored == null ? null : stored.getSetting(SIGNING_SECRET_KEY);

        if (storedSigningSecret != null) {
            secretSettings.put(SIGNING_SECRET_KEY, storedSigningSecret);
        }

        target.setSettings(plainSettings);
        target.setSecretSettings(secretSettings);
    }

    /**
     * Returns the plain settings plus the mask in place of every secret value. The signing secret is never included.
     */
    static Map<String, Object> masked(Notification notification) {
        Map<String, Object> maskedSettings = new HashMap<>();

        for (Map.Entry<String, Object> entry : notification.getSettings()
            .entrySet()) {

            if (!SECRET_KEYS.contains(entry.getKey())) {
                maskedSettings.put(entry.getKey(), entry.getValue());
            }
        }

        if (notification.getSetting(SLACK_WEBHOOK_URL_KEY) != null) {
            maskedSettings.put(SLACK_WEBHOOK_URL_KEY, MASK);
        }

        Object headers = notification.getSetting(WEBHOOK_HEADERS_KEY);

        if (headers instanceof List<?> headerList && !headerList.isEmpty()) {
            List<Map<String, Object>> maskedHeaders = new ArrayList<>();

            for (Object header : headerList) {
                if (header instanceof Map<?, ?> headerMap) {
                    Map<String, Object> maskedHeader = new LinkedHashMap<>();

                    maskedHeader.put("name", headerMap.get("name"));
                    maskedHeader.put("value", MASK);

                    maskedHeaders.add(maskedHeader);
                }
            }

            maskedSettings.put(WEBHOOK_HEADERS_KEY, maskedHeaders);
        }

        return maskedSettings;
    }

    private static List<Map<String, Object>> resolveHeaders(Object incomingHeaders, @Nullable Object storedHeaders) {
        List<Map<String, Object>> resolvedHeaders = new ArrayList<>();

        if (!(incomingHeaders instanceof List<?> incomingHeaderList)) {
            return resolvedHeaders;
        }

        for (Object incomingHeader : incomingHeaderList) {
            if (!(incomingHeader instanceof Map<?, ?> incomingHeaderMap)) {
                continue;
            }

            Object name = incomingHeaderMap.get("name");
            Object value = incomingHeaderMap.get("value");

            if (value == null || MASK.equals(value)) {
                value = findStoredHeaderValue(storedHeaders, name);
            }

            if (name != null && value != null) {
                Map<String, Object> resolvedHeader = new LinkedHashMap<>();

                resolvedHeader.put("name", name);
                resolvedHeader.put("value", value);

                resolvedHeaders.add(resolvedHeader);
            }
        }

        return resolvedHeaders;
    }

    @Nullable
    private static Object findStoredHeaderValue(@Nullable Object storedHeaders, @Nullable Object name) {
        if (!(storedHeaders instanceof List<?> storedHeaderList) || !(name instanceof String nameString)) {
            return null;
        }

        for (Object storedHeader : storedHeaderList) {
            if (storedHeader instanceof Map<?, ?> storedHeaderMap &&
                nameString.equalsIgnoreCase(String.valueOf(storedHeaderMap.get("name")))) {

                return storedHeaderMap.get("value");
            }
        }

        return null;
    }
}
