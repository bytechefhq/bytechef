
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
import com.bytechef.hermes.definition.registry.dto.ConnectionDefinitionBasicDTO;
import com.bytechef.hermes.definition.registry.dto.HelpDTO;
import com.bytechef.hermes.definition.registry.dto.OptionsDataSourceDTO;
import com.bytechef.hermes.definition.registry.dto.PropertyDTO;
import com.bytechef.hermes.definition.registry.dto.ResourcesDTO;
import org.mapstruct.Mapper;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ConfigurationMapperSpringConfig.class)
public interface OptionalMapper {

    default ConnectionDefinitionBasicDTO mapToConnectionDefinitionBasicDTO(
        Optional<ConnectionDefinitionBasicDTO> value) {

        return value.orElse(null);
    }

    default HelpDTO mapToHelpDTO(Optional<HelpDTO> value) {
        return value.orElse(null);
    }

    default Integer mapToInteger(Optional<Integer> value) {
        return value.orElse(null);
    }

    default OptionsDataSourceDTO mapToOptionsDataSourceDTO(Optional<OptionsDataSourceDTO> value) {
        return value.orElse(null);
    }

    default PropertyDTO mapToPropertyDTO(Optional<PropertyDTO> value) {
        return value.orElse(null);
    }

    default ResourcesDTO mapToResourcesModel(Optional<ResourcesDTO> value) {
        return value.orElse(null);
    }

    default String mapToString(Optional<String> value) {
        return value.orElse(null);
    }
}
