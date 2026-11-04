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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
final class NotificationSecretSettings {

    static final String MASK = "********";
    static final String SIGNING_SECRET_KEY = "webhookSecret";
    static final String SLACK_WEBHOOK_URL_KEY = "slackWebhookUrl";
    static final String WEBHOOK_HEADERS_KEY = "webhookHeaders";
    static final String WEBHOOK_URL_KEY = "webhook";
    static final Set<String> SECRET_KEYS = Set.of(SLACK_WEBHOOK_URL_KEY, WEBHOOK_HEADERS_KEY, SIGNING_SECRET_KEY);

    private NotificationSecretSettings() {
    }

    /**
     * Moves the secret keys of the incoming settings into the secret settings of the target for a save. A secret sent
     * back as the mask, or omitted, keeps the stored value; a blank Slack URL counts as omitted. A masked or omitted
     * header value without a stored header of the same name is rejected, and so is any masked or omitted header value
     * when the webhook URL differs from the stored one. The signing secret is never taken from the incoming settings
     * and a save always keeps the stored one.
     */
    static void apply(Notification target, Map<String, Object> incomingSettings, @Nullable Notification stored) {
        apply(target, incomingSettings, stored, false);
    }

    /**
     * Same as {@link #apply(Notification, Map, Notification)}, except that a test to a webhook URL that differs from
     * the stored one does not get the stored signing secret, so it goes out unsigned.
     */
    static void applyForTest(
        Notification target, Map<String, Object> incomingSettings, @Nullable Notification stored) {

        apply(target, incomingSettings, stored, true);
    }

    static boolean isWebhookUrlChanged(@Nullable Map<String, Object> incomingSettings, @Nullable Notification stored) {
        if (stored == null || stored.getType() != Notification.Type.WEBHOOK) {
            return false;
        }

        Object incomingUrl = incomingSettings == null ? null : incomingSettings.get(WEBHOOK_URL_KEY);
        Object storedUrl = stored.getSetting(WEBHOOK_URL_KEY);

        return !normalizeUrl(incomingUrl).equals(normalizeUrl(storedUrl));
    }

    static String normalizeUrl(@Nullable Object url) {
        if (!(url instanceof String urlString)) {
            return "";
        }

        String strippedUrl = urlString.strip();

        URI uri;

        try {
            uri = new URI(strippedUrl);
        } catch (URISyntaxException uriSyntaxException) {
            return strippedUrl;
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (scheme == null || host == null) {
            return strippedUrl;
        }

        scheme = scheme.toLowerCase(Locale.ROOT);

        int port = uri.getPort();

        if (("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443)) {
            port = -1;
        }

        StringBuilder normalizedUrl = new StringBuilder(scheme).append("://");

        String rawUserInfo = uri.getRawUserInfo();

        if (rawUserInfo != null) {
            normalizedUrl.append(rawUserInfo)
                .append('@');
        }

        normalizedUrl.append(host.toLowerCase(Locale.ROOT));

        if (port != -1) {
            normalizedUrl.append(':')
                .append(port);
        }

        String rawPath = uri.getRawPath();

        if (rawPath != null) {
            normalizedUrl.append(rawPath);
        }

        String rawQuery = uri.getRawQuery();

        if (rawQuery != null) {
            normalizedUrl.append('?')
                .append(rawQuery);
        }

        String rawFragment = uri.getRawFragment();

        if (rawFragment != null) {
            normalizedUrl.append('#')
                .append(rawFragment);
        }

        return normalizedUrl.toString();
    }

    private static void apply(
        Notification target, Map<String, Object> incomingSettings, @Nullable Notification stored, boolean test) {

        Map<String, Object> incoming = incomingSettings == null ? Map.of() : incomingSettings;
        Map<String, Object> plainSettings = new HashMap<>();
        Map<String, Object> secretSettings = new HashMap<>();

        for (Map.Entry<String, Object> entry : incoming.entrySet()) {
            if (!SECRET_KEYS.contains(entry.getKey())) {
                plainSettings.put(entry.getKey(), entry.getValue());
            }
        }

        boolean slack = target.getType() == Notification.Type.SLACK;
        boolean webhook = target.getType() == Notification.Type.WEBHOOK;

        Object storedSlackUrl = stored == null ? null : stored.getSetting(SLACK_WEBHOOK_URL_KEY);

        if (isBlank(storedSlackUrl)) {
            storedSlackUrl = null;
        }

        if (!slack) {
            storedSlackUrl = null;
        } else if (incoming.containsKey(SLACK_WEBHOOK_URL_KEY)) {
            Object slackUrl = incoming.get(SLACK_WEBHOOK_URL_KEY);

            if (MASK.equals(slackUrl) || isBlank(slackUrl)) {
                slackUrl = storedSlackUrl;
            }

            if (slackUrl != null) {
                secretSettings.put(SLACK_WEBHOOK_URL_KEY, slackUrl);
            }
        } else if (storedSlackUrl != null) {
            secretSettings.put(SLACK_WEBHOOK_URL_KEY, storedSlackUrl);
        }

        Object storedHeaders = stored == null ? null : stored.getSetting(WEBHOOK_HEADERS_KEY);

        boolean urlChanged = webhook && isWebhookUrlChanged(incoming, stored);
        boolean reenterHeaders = urlChanged && storedHeaders instanceof List<?> storedHeaderList &&
            !storedHeaderList.isEmpty();

        if (!webhook) {
            storedHeaders = null;
        } else if (incoming.containsKey(WEBHOOK_HEADERS_KEY)) {
            List<Map<String, Object>> headers = resolveHeaders(
                incoming.get(WEBHOOK_HEADERS_KEY), storedHeaders, reenterHeaders);

            if (!headers.isEmpty()) {
                secretSettings.put(WEBHOOK_HEADERS_KEY, headers);
            }
        } else if (reenterHeaders) {
            throw new IllegalArgumentException("Re-enter the headers to send to a new URL");
        } else if (storedHeaders != null) {
            secretSettings.put(WEBHOOK_HEADERS_KEY, storedHeaders);
        }

        Object storedSigningSecret = stored == null || !webhook ? null : stored.getSetting(SIGNING_SECRET_KEY);

        if (storedSigningSecret != null && !(test && urlChanged)) {
            secretSettings.put(SIGNING_SECRET_KEY, storedSigningSecret);
        }

        target.setSettings(plainSettings);
        target.setSecretSettings(secretSettings);
    }

    static Map<String, Object> masked(Notification notification) {
        Map<String, Object> maskedSettings = new HashMap<>();

        for (Map.Entry<String, Object> entry : notification.getSettings()
            .entrySet()) {

            if (!SECRET_KEYS.contains(entry.getKey())) {
                maskedSettings.put(entry.getKey(), entry.getValue());
            }
        }

        if (notification.getType() == Notification.Type.SLACK
            && !isBlank(notification.getSetting(SLACK_WEBHOOK_URL_KEY))) {
            maskedSettings.put(SLACK_WEBHOOK_URL_KEY, MASK);
        }

        Object headers = notification.getSetting(WEBHOOK_HEADERS_KEY);

        if (notification.getType() == Notification.Type.WEBHOOK && headers instanceof List<?> headerList &&
            !headerList.isEmpty()) {
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

    static boolean hasSigningSecret(Notification notification) {
        return notification.getType() == Notification.Type.WEBHOOK &&
            notification.getSetting(SIGNING_SECRET_KEY) != null;
    }

    private static boolean isBlank(@Nullable Object value) {
        return value == null || (value instanceof String string && string.isBlank());
    }

    private static List<Map<String, Object>> resolveHeaders(
        Object incomingHeaders, @Nullable Object storedHeaders, boolean reenterHeaders) {

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
                if (reenterHeaders && name != null) {
                    throw new IllegalArgumentException("Re-enter header %s to send to a new URL".formatted(name));
                }

                value = findStoredHeaderValue(storedHeaders, name);

                if (name != null && value == null) {
                    throw new IllegalArgumentException("Header %s has no saved value".formatted(name));
                }
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
