
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
import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.HelpDTO;
import com.bytechef.hermes.definition.registry.dto.ResourcesDTO;
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
        extends Converter<ComponentDefinitionDTO, ComponentDefinitionModel> {

        ComponentDefinitionModel convert(ComponentDefinitionDTO componentDefinitionDTO);

        ConnectionDefinitionBasicModel map(ConnectionDefinitionBasicDTO connectionDefinitionBasicDTO);

        HelpModel map(HelpDTO helpDTO);

        ResourcesModel map(ResourcesDTO resourcesDTO);
    }

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ComponentDefinitionToComponentDefinitionBasicModelMapper
        extends Converter<ComponentDefinitionDTO, ComponentDefinitionBasicModel> {

        ComponentDefinitionBasicModel convert(ComponentDefinitionDTO componentDefinitionDTO);

        ResourcesModel map(ResourcesDTO resourcesDTO);
    }
}
