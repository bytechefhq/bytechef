/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.configuration.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.configuration.domain.notification.Notification;
import com.bytechef.platform.configuration.service.NotificationService;
import com.bytechef.platform.configuration.web.rest.model.NotificationModel;
import java.util.List;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Matija Petanjek
 */
@RestController
@RequestMapping("${openapi.openAPIDefinition.base-path.platform:}/internal")
@ConditionalOnCoordinator
public class NotificationApiController implements NotificationApi {

    private final ConversionService conversionService;
    private final NotificationService notificationService;

    public NotificationApiController(ConversionService conversionService, NotificationService notificationService) {
        this.conversionService = conversionService;
        this.notificationService = notificationService;
    }

    @Override
    public ResponseEntity<List<NotificationModel>> getNotifications() {
        return ResponseEntity.ok(
            notificationService.findAll()
                .stream()
                .map(notification -> conversionService.convert(notification, NotificationModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<NotificationModel> postNotification(NotificationModel notificationModel) {
        return ResponseEntity.ok(
            conversionService.convert(
                notificationService.create(
                    notificationModel.getName(),
                    Notification.Type.valueOf(
                        notificationModel.getNotificationType()
                            .toString()),
                    notificationModel.getSettings(),
                    notificationModel.getEvents()
                        .stream()
                        .map(
                            notificationEventModel -> notificationEventModel.getId())
                        .toList()),
                NotificationModel.class));
    }

}
