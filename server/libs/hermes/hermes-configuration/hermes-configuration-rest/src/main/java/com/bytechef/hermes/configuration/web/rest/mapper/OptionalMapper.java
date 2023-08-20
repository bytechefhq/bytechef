
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
import com.bytechef.hermes.definition.registry.domain.Property;
import com.bytechef.hermes.definition.registry.domain.ConnectionDefinitionBasic;
import com.bytechef.hermes.definition.registry.domain.Help;
import com.bytechef.hermes.definition.registry.domain.OptionsDataSource;
import com.bytechef.hermes.definition.registry.domain.Resources;
import org.mapstruct.Mapper;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ConfigurationMapperSpringConfig.class)
public interface OptionalMapper {

    default ConnectionDefinitionBasic mapToConnectionDefinitionBasicDTO(
        Optional<ConnectionDefinitionBasic> value) {

        return value.orElse(null);
    }

    default Help mapToHelpDTO(Optional<Help> value) {
        return value.orElse(null);
    }

    default Integer mapToInteger(Optional<Integer> value) {
        return value.orElse(null);
    }

    default OptionsDataSource mapToOptionsDataSourceDTO(Optional<OptionsDataSource> value) {
        return value.orElse(null);
    }

    default Property mapToPropertyDTO(Optional<Property> value) {
        return value.orElse(null);
    }

    default Resources mapToResourcesModel(Optional<Resources> value) {
        return value.orElse(null);
    }

    default String mapToString(Optional<String> value) {
        return value.orElse(null);
    }
}
