
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

package com.bytechef.helios.configuration.web.rest.mapper;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.task.WorkflowTask;
import com.bytechef.helios.configuration.connection.WorkflowConnection;
import com.bytechef.helios.configuration.web.rest.model.WorkflowConnectionModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowTaskModel;
import com.bytechef.hermes.configuration.web.rest.mapper.config.WorkflowConfigurationMapperSpringConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = WorkflowConfigurationMapperSpringConfig.class)
public interface ProjectWorkflowMapper extends Converter<Workflow, WorkflowModel> {

    @Override
    @Mapping(target = "connections", ignore = true)
    WorkflowModel convert(Workflow workflow);

    WorkflowTaskModel map(WorkflowTask workflowTask);

    List<WorkflowConnectionModel> map(List<WorkflowConnection> workflowConnections);

    default Integer mapTointeger(Optional<Integer> optional) {
        return optional.orElse(null);
    }

    default String mapToString(Optional<String> optional) {
        return optional.orElse(null);
    }

    @AfterMapping
    default void afterMapping(Workflow workflow, @MappingTarget WorkflowModel workflowModel) {
        workflowModel.connections(map(WorkflowConnection.of(workflow)));
    }
}
