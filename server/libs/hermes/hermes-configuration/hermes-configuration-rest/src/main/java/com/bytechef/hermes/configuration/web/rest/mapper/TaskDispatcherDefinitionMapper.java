
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
import com.bytechef.hermes.registry.domain.Resources;
import com.bytechef.hermes.task.dispatcher.registry.domain.TaskDispatcherDefinition;
import com.bytechef.hermes.configuration.web.rest.model.ResourcesModel;
import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherDefinitionBasicModel;
import com.bytechef.hermes.configuration.web.rest.model.TaskDispatcherDefinitionModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class TaskDispatcherDefinitionMapper {

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface TaskDispatcherDefinitionToTaskDispatcherDefinitionModelMapper
        extends Converter<TaskDispatcherDefinition, TaskDispatcherDefinitionModel> {

        @Override
        TaskDispatcherDefinitionModel convert(TaskDispatcherDefinition taskDispatcherDefinition);

        ResourcesModel map(Resources resources);
    }

    @Mapper(config = ConfigurationMapperSpringConfig.class, uses = {
        OptionalMapper.class
    })
    public interface ModifiableTaskDispatcherDefinitionToTaskDispatcherDefinitionModelMapper
        extends Converter<TaskDispatcherDefinition, TaskDispatcherDefinitionBasicModel> {

        @Override
        TaskDispatcherDefinitionBasicModel convert(TaskDispatcherDefinition taskDispatcherDefinition);

        ResourcesModel map(Resources resources);
    }
}
