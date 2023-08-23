
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

package com.bytechef.hermes.configuration.web.rest.mapper;

import com.bytechef.hermes.configuration.web.rest.mapper.config.ConfigurationMapperSpringConfig;
import com.bytechef.hermes.definition.registry.domain.ComponentDefinition;
import com.bytechef.hermes.definition.registry.domain.ConnectionDefinitionBasic;
import com.bytechef.hermes.definition.registry.domain.Help;
import com.bytechef.hermes.definition.registry.domain.Resources;
import com.bytechef.hermes.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.ComponentDefinitionModel;
import com.bytechef.hermes.configuration.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.HelpModel;
import com.bytechef.hermes.configuration.web.rest.model.ResourcesModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

public class ComponentDefinitionMapper {

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ComponentDefinitionToComponentDefinitionModelMapper
        extends Converter<ComponentDefinition, ComponentDefinitionModel> {

        ComponentDefinitionModel convert(ComponentDefinition componentDefinition);

        ConnectionDefinitionBasicModel map(ConnectionDefinitionBasic connectionDefinitionBasic);

        HelpModel map(Help help);

        ResourcesModel map(Resources resources);
    }

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ComponentDefinitionToComponentDefinitionBasicModelMapper
        extends Converter<ComponentDefinition, ComponentDefinitionBasicModel> {

        ComponentDefinitionBasicModel convert(ComponentDefinition componentDefinition);

        ResourcesModel map(Resources resources);
    }
}
