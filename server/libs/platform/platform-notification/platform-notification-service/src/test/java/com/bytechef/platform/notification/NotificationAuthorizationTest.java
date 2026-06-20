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

package com.bytechef.platform.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.facade.NotificationFacadeImpl;
import com.bytechef.platform.notification.service.NotificationServiceImpl;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that close notification IDOR (T24). Notifications are platform-settings
 * configuration (no per-user/workspace scope), so management operations require tenant admin. The dispatcher path
 * {@code getNotifications(NotificationEvent.Type)} runs from a job-status event listener with no
 * {@code SecurityContext} and stays ungated -- a negative assertion locks that in.
 *
 * @author Ivica Cardic
 */
class NotificationAuthorizationTest {

    @Test
    void testGetNotificationsRequiresTenantAdmin() {
        assertAdmin(NotificationFacadeImpl.class, "getNotifications");
    }

    @Test
    void testCreateNotificationRequiresTenantAdmin() {
        assertAdmin(NotificationFacadeImpl.class, "createNotification");
    }

    @Test
    void testUpdateNotificationRequiresTenantAdmin() {
        assertAdmin(NotificationFacadeImpl.class, "updateNotification");
    }

    @Test
    void testDeleteRequiresTenantAdmin() {
        assertAdmin(NotificationServiceImpl.class, "delete");
    }

    @Test
    void testDispatcherGetNotificationsByEventTypeIsNotGated() throws NoSuchMethodException {
        Method method =
            NotificationServiceImpl.class.getDeclaredMethod("getNotifications", NotificationEvent.Type.class);

        assertThat(method.isAnnotationPresent(PreAuthorize.class))
            .as("dispatcher getNotifications(eventType) must NOT carry @PreAuthorize")
            .isFalse();
    }

    private static void assertAdmin(Class<?> clazz, String methodName) {
        Method match = null;

        for (Method candidate : clazz.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName) && candidate.isAnnotationPresent(PreAuthorize.class)) {

                match = candidate;

                break;
            }
        }

        assertThat(match)
            .as("@PreAuthorize-annotated method %s on %s", methodName, clazz.getSimpleName())
            .isNotNull();
        assertThat(match.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasPermission('Tenant', 'ADMIN')");
    }
}
