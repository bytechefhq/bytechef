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

package com.bytechef.automation.configuration.web.rest.mapper;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.automation.configuration.dto.WorkflowDTO;
import com.bytechef.automation.configuration.web.rest.mapper.config.AutomationConfigurationMapperSpringConfig;
import com.bytechef.automation.configuration.web.rest.model.WorkflowBasicModel;
import com.bytechef.automation.configuration.web.rest.model.WorkflowModel;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public abstract class ProjectWorkflowMapper {

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public abstract static class ProjectWorkflowDTOToWorkflowModelMapper
        implements Converter<WorkflowDTO, WorkflowModel> {

        @Override
        public abstract WorkflowModel convert(WorkflowDTO workflowDTO);
    }

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public abstract static class ProjectWorkflowModelToWorkflowBasicModel
        implements Converter<WorkflowDTO, WorkflowBasicModel> {

        @Autowired
        private WorkflowConnectionFacade workflowConnectionFacade;

        @Override
        @Mapping(target = "connectionsCount", ignore = true)
        @Mapping(target = "inputsCount", ignore = true)
        @Mapping(target = "manualTrigger", ignore = true)
        @Mapping(target = "workflowTaskComponentNames", ignore = true)
        @Mapping(target = "workflowTriggerComponentNames", ignore = true)
        public abstract WorkflowBasicModel convert(WorkflowDTO workflowDTO);

        // TODO Find a way to share this code with IntegrationWorkflowMapper, probably to add common interface to
        // WorkflowBasicModels via generator
        @AfterMapping
        public void afterMapping(WorkflowDTO workflowDTO, @MappingTarget WorkflowBasicModel workflowBasicModel) {
            List<WorkflowTask> workflowTasks = workflowDTO.workflow()
                .getAllTasks();
            List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflowDTO.workflow());

            workflowBasicModel.setConnectionsCount(
                (int) getWorkflowTaskConnectionsCount(workflowTasks) +
                    (int) getWorkflowTriggerConnectionsCount(workflowTriggers));
            workflowBasicModel.setInputsCount(CollectionUtils.size(workflowDTO.inputs()));
            workflowBasicModel.setManualTrigger(
                CollectionUtils.isEmpty(workflowTriggers) ||
                    CollectionUtils.contains(
                        CollectionUtils.map(workflowTriggers, WorkflowTrigger::getName),
                        "manual"));
            workflowBasicModel.setWorkflowTaskComponentNames(
                workflowTasks
                    .stream()
                    .map(workflowTask -> WorkflowNodeType.ofType(workflowTask.getType()))
                    .map(WorkflowNodeType::componentName)
                    .toList());

            List<String> workflowTriggerComponentNames = workflowTriggers
                .stream()
                .map(workflowTrigger -> WorkflowNodeType.ofType(workflowTrigger.getType()))
                .map(WorkflowNodeType::componentName)
                .toList();

            workflowBasicModel.setWorkflowTriggerComponentNames(
                workflowTriggerComponentNames.isEmpty() ? List.of("manual") : workflowTriggerComponentNames);
        }

        private long getWorkflowTaskConnectionsCount(List<WorkflowTask> workflowTasks) {
            return workflowTasks
                .stream()
                .flatMap(workflowTask -> CollectionUtils.stream(
                    workflowConnectionFacade.getWorkflowConnections(workflowTask)))
                .count();
        }

        private long getWorkflowTriggerConnectionsCount(List<WorkflowTrigger> workflowTriggers) {
            return workflowTriggers
                .stream()
                .flatMap(workflowTrigger -> CollectionUtils.stream(
                    workflowConnectionFacade.getWorkflowConnections(workflowTrigger)))
                .count();
        }
    }
}
