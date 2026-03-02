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

package com.bytechef.platform.workflow.execution.web.rest.mapper;

import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.web.rest.mapper.config.PlatformWorkflowExecutionMapperSpringConfig;
import com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionModel;
import java.util.List;
import java.util.Optional;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = PlatformWorkflowExecutionMapperSpringConfig.class)
public interface TaskExecutionMapper extends Converter<TaskExecutionDTO, TaskExecutionModel> {

    @Override
    @Named(value = "taskExecutionDTOToTaskExecutionModelMapper")
    TaskExecutionModel convert(TaskExecutionDTO taskExecutionDTO);

    @IterableMapping(qualifiedByName = "taskExecutionDTOToTaskExecutionModelMapper")
    List<TaskExecutionModel> convertList(List<TaskExecutionDTO> list);

    default String map(Optional<String> optional) {
        return optional.orElse(null);
    }
}
