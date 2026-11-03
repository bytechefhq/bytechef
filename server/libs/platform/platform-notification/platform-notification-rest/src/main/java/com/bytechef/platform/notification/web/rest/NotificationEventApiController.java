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

package com.bytechef.platform.notification.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.platform.notification.facade.NotificationFacade;
import com.bytechef.platform.notification.web.rest.model.NotificationEventModel;
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
public class NotificationEventApiController implements NotificationEventApi {

    private final ConversionService conversionService;
    private final NotificationFacade notificationFacade;

    public NotificationEventApiController(
        ConversionService conversionService, NotificationFacade notificationFacade) {

        this.conversionService = conversionService;
        this.notificationFacade = notificationFacade;
    }

    @Override
    public ResponseEntity<List<NotificationEventModel>> getNotificationEvents() {
        return ResponseEntity.ok(
            notificationFacade.getNotificationEvents()
                .stream()
                .map(notificationEvent -> conversionService.convert(notificationEvent, NotificationEventModel.class))
                .toList());
    }
}
