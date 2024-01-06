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

package com.bytechef.hermes.configuration.web.rest.mapper;

import com.bytechef.hermes.component.registry.domain.ConnectionDefinition;
import com.bytechef.hermes.component.registry.domain.ConnectionDefinitionBasic;
import com.bytechef.hermes.configuration.web.rest.mapper.config.ConfigurationMapperSpringConfig;
import com.bytechef.hermes.configuration.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.ConnectionDefinitionModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ConnectionDefinitionMapper {

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ConnectionDefinitionBasicToConnectionDefinitionModelMapper
        extends Converter<ConnectionDefinitionBasic, ConnectionDefinitionBasicModel> {

        @Override
        ConnectionDefinitionBasicModel convert(ConnectionDefinitionBasic connectionDefinition);
    }

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ConnectionDefinitionToConnectionDefinitionModelMapper
        extends Converter<ConnectionDefinition, ConnectionDefinitionModel> {

        @Override
        @Mapping(target = "baseUri", ignore = true)
        ConnectionDefinitionModel convert(ConnectionDefinition connectionDefinition);
    }

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ConnectionDefinitionToConnectionDefinitionBasicModelMapper
        extends Converter<ConnectionDefinition, ConnectionDefinitionBasicModel> {

        @Override
        ConnectionDefinitionBasicModel convert(ConnectionDefinition connectionDefinition);
    }
}
