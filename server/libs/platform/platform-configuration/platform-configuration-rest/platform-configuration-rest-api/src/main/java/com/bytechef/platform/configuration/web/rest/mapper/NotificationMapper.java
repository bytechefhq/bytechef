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

package com.bytechef.platform.configuration.web.rest.mapper;

import com.bytechef.platform.configuration.domain.notification.Event;
import com.bytechef.platform.configuration.domain.notification.Notification;
import com.bytechef.platform.configuration.service.EventService;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.NotificationEventModel;
import com.bytechef.platform.configuration.web.rest.model.NotificationModel;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Matija Petanjek
 */
@Mapper(config = PlatformConfigurationMapperSpringConfig.class)
public abstract class NotificationMapper implements Converter<Notification, NotificationModel> {

    @Autowired
    protected EventService eventService;

    @Mapping(source = "type", target = "notificationType")
    @Override
    public abstract NotificationModel convert(Notification notification);

    @AfterMapping
    protected void afterMapping(@MappingTarget NotificationModel notificationModel, Notification notification) {
        List<Event> events = eventService.findAllIn(notification.getEventIds());
        notificationModel.setEvents(events.stream()
            .map(event -> new NotificationEventModel(event.getId())
                .type(NotificationEventModel.TypeEnum.valueOf(event.getType()
                    .toString())))
            .toList());
    }
}
