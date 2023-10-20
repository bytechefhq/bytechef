
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
import com.bytechef.helios.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.helios.configuration.web.rest.model.WorkflowConnectionModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowTaskModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowTriggerModel;
import com.bytechef.hermes.configuration.trigger.WorkflowTrigger;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectConfigurationMapperSpringConfig.class)
public interface ProjectWorkflowMapper extends Converter<Workflow, WorkflowModel> {

    @Override
    @Mapping(target = "triggers", ignore = true)
    WorkflowModel convert(Workflow workflow);

    @Mapping(target = "connections", ignore = true)
    WorkflowTaskModel map(WorkflowTask workflowTask);

    @Mapping(target = "connections", ignore = true)
    WorkflowTriggerModel map(WorkflowTrigger workflowTrigger);

    List<WorkflowConnectionModel> mapWorkflowConnections(List<WorkflowConnection> workflowConnections);

    List<WorkflowTriggerModel> mapWorkflowTriggers(List<WorkflowTrigger> workflowTriggers);

    default Integer mapTointeger(Optional<Integer> optional) {
        return optional.orElse(null);
    }

    default String mapToString(Optional<String> optional) {
        return optional.orElse(null);
    }

    @AfterMapping
    default void afterMapping(Workflow workflow, @MappingTarget WorkflowModel workflowModel) {
        for (WorkflowTaskModel workflowTaskModel : workflowModel.getTasks()) {
            workflowTaskModel.connections(
                mapWorkflowConnections(
                    WorkflowConnection.of(
                        workflow.getTasks()
                            .stream()
                            .filter(workflowTask -> Objects.equals(workflowTask.getName(), workflowTaskModel.getName()))
                            .findFirst()
                            .orElseThrow())));
        }

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        List<WorkflowTriggerModel> workflowTriggerModels = mapWorkflowTriggers(workflowTriggers);

        for (WorkflowTriggerModel workflowTriggerModel : workflowTriggerModels) {
            workflowTriggerModel.connections(
                mapWorkflowConnections(
                    WorkflowConnection.of(
                        workflowTriggers
                            .stream()
                            .filter(
                                workflowTrigger -> Objects.equals(
                                    workflowTrigger.getName(), workflowTriggerModel.getName()))
                            .findFirst()
                            .orElseThrow())));
        }

        workflowModel.triggers(workflowTriggerModels);
    }
}
