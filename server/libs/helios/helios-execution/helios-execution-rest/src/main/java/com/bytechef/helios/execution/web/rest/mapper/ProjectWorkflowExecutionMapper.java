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

package com.bytechef.helios.execution.web.rest.mapper;

import com.bytechef.helios.execution.dto.WorkflowExecution;
import com.bytechef.helios.execution.web.rest.mapper.config.ProjectExecutionMapperSpringConfig;
import com.bytechef.helios.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.bytechef.helios.execution.web.rest.model.WorkflowExecutionModel;
import com.bytechef.hermes.configuration.web.rest.mapper.OptionalMapper;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ProjectWorkflowExecutionMapper {

    @Mapper(config = ProjectExecutionMapperSpringConfig.class, uses = OptionalMapper.class)
    public interface ProjectWorkflowExecutionDTOToWorkflowExecutionModelMapper
        extends Converter<WorkflowExecution, WorkflowExecutionModel> {

        @Override
        WorkflowExecutionModel convert(WorkflowExecution workflowExecution);
    }

    @Mapper(config = ProjectExecutionMapperSpringConfig.class)
    public interface ProjectWorkflowExecutionDTOToWorkflowExecutionBasicModelMapper
        extends Converter<WorkflowExecution, WorkflowExecutionBasicModel> {

        @Override
        WorkflowExecutionBasicModel convert(WorkflowExecution workflowExecution);
    }
}
