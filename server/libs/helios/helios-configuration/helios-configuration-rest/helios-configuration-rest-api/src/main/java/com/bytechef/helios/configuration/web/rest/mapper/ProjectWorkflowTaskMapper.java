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

package com.bytechef.helios.configuration.web.rest.mapper;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.helios.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.helios.configuration.web.rest.model.WorkflowTaskModel;
import java.util.List;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectConfigurationMapperSpringConfig.class)
public interface ProjectWorkflowTaskMapper extends Converter<WorkflowTask, WorkflowTaskModel> {

    @Named(value = "workflowTaskToWorkflowTaskModelMapper")
    @Mapping(target = "connections", ignore = true)
    WorkflowTaskModel convert(WorkflowTask workflowTask);

    @IterableMapping(qualifiedByName = "workflowTaskToWorkflowTaskModelMapper")
    List<WorkflowTaskModel> map(List<WorkflowTask> workflowTasks);
}
