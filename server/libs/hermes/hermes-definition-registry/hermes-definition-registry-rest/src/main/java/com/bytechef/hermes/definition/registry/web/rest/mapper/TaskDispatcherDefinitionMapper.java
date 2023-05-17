
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

import com.bytechef.hermes.definition.registry.dto.ResourcesDTO;
import com.bytechef.hermes.definition.registry.dto.TaskDispatcherDefinitionDTO;
import com.bytechef.hermes.definition.registry.web.rest.mapper.config.DefinitionMapperSpringConfig;
import com.bytechef.hermes.definition.registry.web.rest.model.ResourcesModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TaskDispatcherDefinitionBasicModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TaskDispatcherDefinitionModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = DefinitionMapperSpringConfig.class)
public class TaskDispatcherDefinitionMapper {

    @Mapper(config = DefinitionMapperSpringConfig.class)
    public interface TaskDispatcherDefinitionToTaskDispatcherDefinitionModelMapper
        extends Converter<TaskDispatcherDefinitionDTO, TaskDispatcherDefinitionModel> {

        @Override
        TaskDispatcherDefinitionModel convert(TaskDispatcherDefinitionDTO taskDispatcherDefinitionDTO);

        ResourcesModel map(ResourcesDTO resourcesDTO);

        default ResourcesModel mapToResource(Optional<ResourcesDTO> optional) {
            return optional.map(this::map)
                .orElse(null);
        }

        default String mapToString(Optional<String> value) {
            return value.orElse(null);
        }
    }

    @Mapper(config = DefinitionMapperSpringConfig.class)
    public interface ModifiableTaskDispatcherDefinitionToTaskDispatcherDefinitionModelMapper
        extends Converter<TaskDispatcherDefinitionDTO, TaskDispatcherDefinitionBasicModel> {

        @Override
        TaskDispatcherDefinitionBasicModel convert(TaskDispatcherDefinitionDTO taskDispatcherDefinitionDTO);

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
