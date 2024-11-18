/*
 * Copyright 2023-present ByteChef Inc.
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
import com.bytechef.platform.workflow.execution.web.rest.mapper.config.WorkflowExecutionMapperSpringConfig;
import com.bytechef.platform.workflow.execution.web.rest.model.TaskExecutionModel;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = WorkflowExecutionMapperSpringConfig.class)
public interface TaskExecutionMapper extends Converter<TaskExecutionDTO, TaskExecutionModel> {

    @Override
    TaskExecutionModel convert(TaskExecutionDTO taskExecutionDTO);

    default String map(Optional<String> optional) {
        return optional.orElse(null);
    }
}
