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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.helios.configuration.web.rest.model.WorkflowModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowTaskModel;
import com.bytechef.helios.configuration.web.rest.model.WorkflowTriggerModel;
import com.bytechef.hermes.configuration.domain.WorkflowTrigger;
import com.bytechef.hermes.configuration.facade.WorkflowConnectionFacade;
import java.util.List;
import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectConfigurationMapperSpringConfig.class)
public abstract class ProjectWorkflowMapper implements Converter<Workflow, WorkflowModel> {

    @Autowired
    private ProjectWorkflowConnectionMapper projectWorkflowConnectionMapper;

    @Autowired
    private ProjectWorkflowTriggerMapper projectWorkflowTriggerMapper;

    @Autowired
    private WorkflowConnectionFacade workflowConnectionFacade;

    @Override
    @Mapping(target = "triggers", ignore = true)
    public abstract WorkflowModel convert(Workflow workflow);

    @AfterMapping
    public void afterMapping(Workflow workflow, @MappingTarget WorkflowModel workflowModel) {
        for (WorkflowTaskModel workflowTaskModel : workflowModel.getTasks()) {
            workflowTaskModel.connections(
                CollectionUtils.map(
                    workflowConnectionFacade.getWorkflowConnections(
                        CollectionUtils.getFirst(
                            workflow.getTasks(),
                            workflowTask -> Objects.equals(workflowTask.getName(), workflowTaskModel.getName()))),
                    workflowConnection -> projectWorkflowConnectionMapper.convert(workflowConnection)));
        }

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        List<WorkflowTriggerModel> workflowTriggerModels = CollectionUtils.map(
            workflowTriggers, workflowTrigger -> projectWorkflowTriggerMapper.convert(workflowTrigger));

        for (WorkflowTriggerModel workflowTriggerModel : workflowTriggerModels) {
            workflowTriggerModel.connections(
                CollectionUtils.map(
                    workflowConnectionFacade.getWorkflowConnections(
                        CollectionUtils.getFirst(
                            workflowTriggers,
                            workflowTrigger -> Objects.equals(
                                workflowTrigger.getName(), workflowTriggerModel.getName()))),
                    workflowConnection -> projectWorkflowConnectionMapper.convert(workflowConnection)));
        }

        workflowModel.triggers(workflowTriggerModels);
    }
}
