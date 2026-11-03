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

package com.bytechef.platform.notification.dto;

import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.Notification.Type;
import com.bytechef.platform.notification.domain.NotificationEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record NotificationDTO(
    Long id, String name, Type type, Map<String, Object> settings, List<NotificationEvent> notificationEvents,
    List<Long> notificationEventIds,
    String createdBy, Instant createdDate, String lastModifiedBy, Instant lastModifiedDate, int version,
    boolean hasSigningSecret, @Nullable String signingSecret) {

    /**
     * @param settings      the masked settings view, never the stored secrets
     * @param signingSecret the signing secret, set only in the create response
     */
    public NotificationDTO(
        Notification notification, Map<String, Object> settings, List<NotificationEvent> notificationEvents,
        boolean hasSigningSecret, @Nullable String signingSecret) {

        this(
            notification.getId(), notification.getName(), notification.getType(), settings,
            notificationEvents, notification.getNotificationEventIds(), notification.getCreatedBy(),
            notification.getCreatedDate(),
            notification.getLastModifiedBy(), notification.getLastModifiedDate(), notification.getVersion(),
            hasSigningSecret, signingSecret);
    }
}
