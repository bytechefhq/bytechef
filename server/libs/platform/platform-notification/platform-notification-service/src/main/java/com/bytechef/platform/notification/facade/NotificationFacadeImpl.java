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

import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.notification.audit.NotificationAuditEvents;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.dto.NotificationDTO;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationEventService;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class NotificationFacadeImpl implements NotificationFacade {

    private static final Pattern HEADER_NAME_PATTERN = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]{1,256}$");
    private static final int MAX_HEADER_COUNT = 20;
    private static final int MAX_HEADER_VALUE_LENGTH = 4096;
    private static final String RESERVED_HEADER_PREFIX = "x-bytechef-";
    private static final Set<String> RESERVED_HEADER_NAMES = Set.of(
        "content-type", "content-length", "host", "transfer-encoding", "connection");

    private final NotificationEventService notificationEventService;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;

    @SuppressFBWarnings("EI")
    public NotificationFacadeImpl(
        NotificationEventService notificationEventService, NotificationSenderRegistry notificationSenderRegistry,
        NotificationService notificationService) {

        this.notificationEventService = notificationEventService;
        this.notificationSenderRegistry = notificationSenderRegistry;
        this.notificationService = notificationService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<NotificationDTO> getNotifications() {
        return notificationService.getNotifications()
            .stream()
            .map(notification -> toDTO(notification, null))
            .toList();
    }

    @Override
    @Audited(event = NotificationAuditEvents.NOTIFICATION_CREATED, data = {
        @AuditData(key = "notificationId", value = "#result?.id"),
        @AuditData(key = "name", value = "#notification.name"),
        @AuditData(key = "type", value = "#notification.type")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public NotificationDTO createNotification(Notification notification) {
        validateNotification(notification);

        NotificationSecretSettings.apply(notification, notification.getSettings(), null);

        String signingSecret = null;

        if (notification.getType() == Notification.Type.WEBHOOK) {
            signingSecret = WebhookSigningSecrets.generate();

            putSecretSetting(notification, NotificationSecretSettings.SIGNING_SECRET_KEY, signingSecret);
        }

        notification = notificationService.create(notification);

        return toDTO(notification, signingSecret);
    }

    @Override
    @Audited(event = NotificationAuditEvents.NOTIFICATION_UPDATED, data = {
        @AuditData(key = "notificationId", value = "#notification.id"),
        @AuditData(key = "name", value = "#notification.name"),
        @AuditData(key = "type", value = "#notification.type")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public NotificationDTO updateNotification(Notification notification) {
        validateNotification(notification);

        Notification storedNotification = notificationService.getNotification(notification.getId());

        NotificationSecretSettings.apply(notification, notification.getSettings(), storedNotification);

        notification = notificationService.update(notification);

        return toDTO(notification, null);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String regenerateSigningSecret(long id) {
        Notification notification = notificationService.getNotification(id);

        if (notification.getType() != Notification.Type.WEBHOOK) {
            throw new IllegalArgumentException("Only webhook notifications have a signing secret");
        }

        String signingSecret = WebhookSigningSecrets.generate();

        putSecretSetting(notification, NotificationSecretSettings.SIGNING_SECRET_KEY, signingSecret);

        notificationService.update(notification);

        return signingSecret;
    }

    @Override
    @Audited(event = NotificationAuditEvents.NOTIFICATION_DELETED, data = {
        @AuditData(key = "notificationId", value = "#id")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deleteNotification(long id) {
        notificationService.delete(id);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<NotificationEvent> getNotificationEvents() {
        return notificationEventService.getNotificationEvents();
    }

    private static void putSecretSetting(Notification notification, String key, Object value) {
        Map<String, Object> secretSettings = new HashMap<>(notification.getSecretSettings());

        secretSettings.put(key, value);

        notification.setSecretSettings(secretSettings);
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = new URI(value.strip());

            String scheme = uri.getScheme();

            return uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme));
        } catch (URISyntaxException uriSyntaxException) {
            return false;
        }
    }

    private NotificationDTO toDTO(Notification notification, String signingSecret) {
        return new NotificationDTO(
            notification, NotificationSecretSettings.masked(notification),
            notificationEventService.getNotificationEvents(notification.getNotificationEventIds()),
            notification.getSetting(NotificationSecretSettings.SIGNING_SECRET_KEY) != null, signingSecret);
    }

    private void validateNotification(Notification notification) {
        Notification.Type type = notification.getType();

        if (!notificationSenderRegistry.hasNotificationSender(type)) {
            throw new IllegalArgumentException(
                "Notification type %s is not available in this edition".formatted(type));
        }

        if (type == Notification.Type.WEBHOOK) {
            validateWebhookUrl(notification.getSettings());
            validateWebhookHeaders(notification.getSettings());
        }
    }

    private static void validateWebhookUrl(Map<String, Object> settings) {
        Object webhookUrl = settings == null ? null : settings.get("webhook");

        if (!(webhookUrl instanceof String webhookUrlString) || webhookUrlString.isBlank()) {
            throw new IllegalArgumentException("Webhook URL is required");
        }

        if (!isHttpUrl(webhookUrlString)) {
            throw new IllegalArgumentException("Webhook URL must be an http or https URL");
        }
    }

    private static void validateWebhookHeaders(Map<String, Object> settings) {
        Object headers = settings.get(NotificationSecretSettings.WEBHOOK_HEADERS_KEY);

        if (headers == null) {
            return;
        }

        if (!(headers instanceof List<?> headerList)) {
            throw new IllegalArgumentException("Webhook headers must be a list");
        }

        if (headerList.size() > MAX_HEADER_COUNT) {
            throw new IllegalArgumentException("A webhook can have at most %d headers".formatted(MAX_HEADER_COUNT));
        }

        Set<String> names = new HashSet<>();

        for (Object header : headerList) {
            if (!(header instanceof Map<?, ?> headerMap) || !(headerMap.get("name") instanceof String name) ||
                !HEADER_NAME_PATTERN.matcher(name)
                    .matches()) {

                throw new IllegalArgumentException("Webhook header name is not valid");
            }

            String lowerCaseName = name.toLowerCase(Locale.ROOT);

            if (lowerCaseName.startsWith(RESERVED_HEADER_PREFIX) || RESERVED_HEADER_NAMES.contains(lowerCaseName)) {
                throw new IllegalArgumentException("Webhook header name %s is reserved".formatted(name));
            }

            if (!names.add(lowerCaseName)) {
                throw new IllegalArgumentException("Webhook header name %s is duplicated".formatted(name));
            }

            Object value = headerMap.get("value");

            if (value != null
                && (!(value instanceof String valueString) || valueString.length() > MAX_HEADER_VALUE_LENGTH)) {
                throw new IllegalArgumentException(
                    "Webhook header %s value must be a string of at most %d characters".formatted(
                        name, MAX_HEADER_VALUE_LENGTH));
            }
        }
    }
}
