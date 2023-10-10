
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

import com.bytechef.hermes.configuration.web.rest.mapper.config.ConfigurationMapperSpringConfig;
import com.bytechef.hermes.component.registry.domain.ActionDefinition;
import com.bytechef.hermes.registry.domain.Help;

import com.bytechef.hermes.configuration.web.rest.model.ActionDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.ActionDefinitionModel;
import com.bytechef.hermes.configuration.web.rest.model.HelpModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ActionDefinitionMapper {

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ActionDefinitionToActionDefinitionModelMapper
        extends Converter<ActionDefinition, ActionDefinitionModel> {

        @Override
        ActionDefinitionModel convert(ActionDefinition actionDefinition);

        HelpModel map(Help help);
    }

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ActionDefinitionToActionDefinitionBasicModelMapper
        extends Converter<ActionDefinition, ActionDefinitionBasicModel> {

        @Override
        ActionDefinitionBasicModel convert(ActionDefinition actionDefinition);

        HelpModel map(Help help);
    }
}
