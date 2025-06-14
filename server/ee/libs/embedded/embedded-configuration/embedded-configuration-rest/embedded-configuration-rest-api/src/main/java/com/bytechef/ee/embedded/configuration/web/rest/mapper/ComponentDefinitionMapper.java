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

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

public class ComponentDefinitionMapper {

    @Mapper(
        config = PlatformConfigurationMapperSpringConfig.class, implementationName = "Embedded<CLASS_NAME>Impl")
    public interface ComponentDefinitionToComponentDefinitionBasicModelMapper
        extends Converter<ComponentDefinition, ComponentDefinitionBasicModel> {

        ComponentDefinitionBasicModel convert(ComponentDefinition componentDefinition);

        @AfterMapping
        default void afterMapping(
            ComponentDefinition componentDefinition,
            @MappingTarget ComponentDefinitionBasicModel componentDefinitionBasicModel) {

            componentDefinitionBasicModel.setIcon("/icons/%s.svg".formatted(componentDefinition.getName()));
        }
    }
}
