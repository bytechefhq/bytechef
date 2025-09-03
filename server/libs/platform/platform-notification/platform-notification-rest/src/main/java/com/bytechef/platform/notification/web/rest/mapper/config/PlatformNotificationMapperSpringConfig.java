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

package com.bytechef.platform.notification.web.rest.mapper.config;

import com.bytechef.platform.notification.web.rest.adapter.PlatformNotificationConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import com.bytechef.web.rest.mapper.JsonNullableMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, JsonNullableMapper.class, PlatformNotificationConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.platform.notification.web.rest.adapter",
    conversionServiceAdapterClassName = "PlatformNotificationConversionServiceAdapter")
public interface PlatformNotificationMapperSpringConfig {
}
