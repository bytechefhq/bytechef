
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

import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.definition.registry.web.rest.mapper.config.DefinitionMapperSpringConfig;
import com.bytechef.hermes.definition.registry.web.rest.model.ComponentDefinitionBasicModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = DefinitionMapperSpringConfig.class)
public interface ComponentDefinitionToComponentDefinitionBasicModelMapper
    extends Converter<ComponentDefinition, ComponentDefinitionBasicModel> {

    @Override
    default ComponentDefinitionBasicModel convert(ComponentDefinition componentDefinition) {
        if (componentDefinition instanceof ComponentDSL.ModifiableComponentDefinition) {
            return map((ComponentDSL.ModifiableComponentDefinition) componentDefinition);
        } else {
            return map(componentDefinition);
        }
    }

    ComponentDefinitionBasicModel map(ComponentDefinition componentDefinition);

    ComponentDefinitionBasicModel map(ComponentDSL.ModifiableComponentDefinition modifiableComponentDefinition);
}
