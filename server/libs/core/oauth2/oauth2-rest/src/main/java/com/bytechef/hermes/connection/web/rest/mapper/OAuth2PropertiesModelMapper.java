
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.connection.web.rest.mapper;

import com.bytechef.hermes.connection.web.rest.mapper.config.OAuth2MapperSpringConfig;
import com.bytechef.oauth2.config.OAuth2Properties;
import com.bytechef.hermes.connection.web.rest.model.OAuth2PropertiesModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@Mapper(config = OAuth2MapperSpringConfig.class)
public interface OAuth2PropertiesModelMapper extends Converter<OAuth2Properties, OAuth2PropertiesModel> {

    @Override
    OAuth2PropertiesModel convert(OAuth2Properties oAuth2Properties);

    default List<String> convert(Map<String, OAuth2Properties.OAuth2App> maps) {
        return maps.keySet()
            .stream()
            .toList();
    }
}
