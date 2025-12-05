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

package com.bytechef.platform.notification.repository;

import com.bytechef.platform.notification.domain.Notification;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Matija Petanjek
 */
@Repository
public interface NotificationRepository extends ListCrudRepository<Notification, Long> {

    @Query("""
            SELECT notification.* FROM notification
            JOIN notification_notification_event ON notification.id = notification_notification_event.notification_id
            JOIN notification_event ON notification_notification_event.event_id = notification_event.id
            WHERE notification_event.type = :type
        """)
    List<Notification> findAllByEventType(@Param("type") int type);
}
