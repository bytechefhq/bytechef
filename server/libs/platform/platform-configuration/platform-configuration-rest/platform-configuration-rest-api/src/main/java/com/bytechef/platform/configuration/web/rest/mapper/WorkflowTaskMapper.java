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

package com.bytechef.platform.configuration.web.rest.mapper;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.WorkflowTaskModel;
import java.util.List;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class WorkflowTaskMapper {

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class, implementationName = "Platform<CLASS_NAME>Impl")
    public interface WorkflowTaskToWorkflowTaskModelMapper extends Converter<WorkflowTask, WorkflowTaskModel> {

        @Named(value = "workflowTaskToWorkflowTaskModelMapper")
        @Mapping(target = "connections", ignore = true)
        @Mapping(target = "destination", ignore = true)
        @Mapping(target = "source", ignore = true)
        WorkflowTaskModel convert(WorkflowTask workflowTask);

        @IterableMapping(qualifiedByName = "workflowTaskToWorkflowTaskModelMapper")
        List<WorkflowTaskModel> map(List<WorkflowTask> workflowTasks);
    }

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class, implementationName = "Platform<CLASS_NAME>Impl")
    public interface WorkflowTaskDTOToWorkflowTaskModelMapper extends Converter<WorkflowTaskDTO, WorkflowTaskModel> {

        @Named(value = "workflowTaskDTOToWorkflowTaskModelMapper")
        WorkflowTaskModel convert(WorkflowTaskDTO workflowTask);

        @IterableMapping(qualifiedByName = "workflowTaskDTOToWorkflowTaskModelMapper")
        List<WorkflowTaskModel> map(List<WorkflowTaskDTO> workflowTaskDTOs);
    }
}
