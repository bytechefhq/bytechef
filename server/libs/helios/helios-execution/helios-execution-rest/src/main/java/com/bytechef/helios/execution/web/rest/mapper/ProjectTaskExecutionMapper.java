
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

package com.bytechef.helios.execution.web.rest.mapper;

import com.bytechef.helios.configuration.web.rest.model.TaskExecutionModel;
import com.bytechef.helios.execution.web.rest.mapper.config.ProjectExecutionMapperSpringConfig;
import com.bytechef.hermes.execution.dto.TaskExecutionDTO;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectExecutionMapperSpringConfig.class)
public interface ProjectTaskExecutionMapper extends Converter<TaskExecutionDTO, TaskExecutionModel> {

    @Override
    TaskExecutionModel convert(TaskExecutionDTO taskExecutionDTO);

    default String map(Optional<String> optional) {
        return optional.orElse(null);
    }
}
