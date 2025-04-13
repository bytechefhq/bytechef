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

import com.bytechef.platform.configuration.domain.Notification;
import com.bytechef.platform.configuration.dto.NotificationDTO;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.NotificationModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Matija Petanjek
 */
@Mapper(config = PlatformConfigurationMapperSpringConfig.class)
public interface NotificationMapper extends Converter<NotificationDTO, NotificationModel> {

    @Override
    @Mapping(target = "notificationEventIds", ignore = true)
    NotificationModel convert(NotificationDTO notification);

    @InheritInverseConfiguration
    @DelegatingConverter
    Notification invertConvert(NotificationModel notificationModel);
}
