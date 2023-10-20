
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

package com.bytechef.hermes.definition.registry.web.rest.mapper;

import com.bytechef.hermes.definition.registry.dto.ComponentDefinitionDTO;
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.HelpDTO;
import com.bytechef.hermes.definition.registry.dto.ResourcesDTO;
import com.bytechef.hermes.definition.registry.web.rest.mapper.config.DefinitionMapperSpringConfig;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ConnectionDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.HelpModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

public class ComponentDefinitionMapper {

    @Mapper(config = DefinitionMapperSpringConfig.class)
    public interface ComponentDefinitionToComponentDefinitionModelMapper
        extends Converter<ComponentDefinitionDTO, ComponentDefinitionModel> {

        ComponentDefinitionModel convert(ComponentDefinitionDTO componentDefinitionDTO);

        ConnectionDefinitionBasicModel map(ConnectionDefinitionBasicDTO connectionDefinitionBasicDTO);

        HelpModel map(HelpDTO helpDTO);

        ResourcesModel map(ResourcesDTO resourcesDTO);

        default ConnectionDefinitionBasicModel mapToConnectionDefinitionBasic(
            Optional<ConnectionDefinitionBasicDTO> optional) {

            return optional.map(this::map)
                .orElse(null);
        }

        default HelpModel mapToHelp(Optional<HelpDTO> optional) {
            return optional.map(this::map)
                .orElse(null);
        }

        default ResourcesModel mapToResource(Optional<ResourcesDTO> optional) {
            return optional.map(this::map)
                .orElse(null);
        }

        default String mapToString(Optional<String> value) {
            return value.orElse(null);
        }
    }

    @Mapper(config = DefinitionMapperSpringConfig.class)
    public interface ComponentDefinitionToComponentDefinitionBasicModelMapper
        extends Converter<ComponentDefinitionDTO, ComponentDefinitionBasicModel> {

        ComponentDefinitionBasicModel convert(ComponentDefinitionDTO componentDefinitionDTO);

        ResourcesModel map(ResourcesDTO resourcesDTO);

        default ResourcesModel mapToResource(Optional<ResourcesDTO> optional) {
            return optional.map(this::map)
                .orElse(null);
        }

        default String mapToString(Optional<String> value) {
            return value.orElse(null);
        }
    }
}
