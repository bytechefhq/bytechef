
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

package com.bytechef.hermes.component.web.rest.mapper;

import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.web.rest.mapper.config.ComponentDefinitionMapperSpringConfig;
import com.bytechef.hermes.component.web.rest.model.GetComponentDefinitionActionDefinitionRequestModel;
import com.bytechef.hermes.component.web.rest.model.GetComponentDefinitionRequestModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ComponentDefinitionMapperSpringConfig.class)
public interface ComponentDefinitionToGetComponentDefinitionRequestModelMapper
    extends Converter<ComponentDefinition, GetComponentDefinitionRequestModel> {

    GetComponentDefinitionRequestModel convert(ComponentDefinition componentDefinition);

    GetComponentDefinitionActionDefinitionRequestModel map(ActionDefinition actionDefinition);
}
