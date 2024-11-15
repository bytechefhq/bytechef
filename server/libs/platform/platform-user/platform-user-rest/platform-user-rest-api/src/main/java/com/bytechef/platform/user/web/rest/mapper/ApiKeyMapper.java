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

package com.bytechef.platform.user.web.rest.mapper;

import com.bytechef.platform.user.domain.ApiKey;
import com.bytechef.platform.user.web.rest.mapper.config.UserMapperSpringConfig;
import com.bytechef.platform.user.web.rest.model.ApiKeyModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = UserMapperSpringConfig.class)
public interface ApiKeyMapper extends Converter<ApiKey, ApiKeyModel> {

    @Override
    ApiKeyModel convert(ApiKey apiKey);

    @InheritInverseConfiguration
    @DelegatingConverter
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "userId", ignore = true)
    ApiKey invertConvert(ApiKeyModel apiKeyModel);
}
