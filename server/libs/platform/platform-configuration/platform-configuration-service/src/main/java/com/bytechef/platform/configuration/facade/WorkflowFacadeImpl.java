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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.dto.WorkflowTriggerDTO;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowFacadeImpl implements WorkflowFacade {

    private final ComponentConnectionFacade componentConnectionFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowFacadeImpl(
        ComponentConnectionFacade componentConnectionFacade, ComponentDefinitionService componentDefinitionService,
        WorkflowService workflowService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.workflowService = workflowService;
    }

    @Override
    public Optional<WorkflowDTO> fetchWorkflow(String id) {
        return workflowService.fetchWorkflow(id)
            .map(this::toWorkflowDTO);
    }

    @Override
    public WorkflowDTO getWorkflow(String id) {
        return toWorkflowDTO(workflowService.getWorkflow(id));
    }

    @Override
    public boolean hasSseStreamResponse(String workflowId) {
        Workflow workflow = workflowService.getWorkflow(workflowId);
        List<WorkflowTask> tasks = workflow.getTasks(true);

        for (WorkflowTask task : tasks) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(task.getType());

            Optional<ComponentDefinition> componentDefinition = componentDefinitionService.fetchComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            if (componentDefinition.isPresent()) {
                boolean hasSseStreamResponse = componentDefinition.get()
                    .getActions()
                    .stream()
                    .filter(actionDefinition -> Objects.equals(
                        actionDefinition.getName(), workflowNodeType.operation()))
                    .anyMatch(com.bytechef.platform.component.domain.ActionDefinition::isSseStreamResponse);

                if (hasSseStreamResponse) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void update(String id, String definition, Integer version) {
        workflowService.update(id, definition, version);
    }

    private WorkflowDTO toWorkflowDTO(Workflow workflow) {
        List<WorkflowTaskDTO> workflowTaskDTOs = new ArrayList<>();

        List<WorkflowTask> allTasks = workflow.getTasks(true);

        for (WorkflowTask workflowTask : allTasks) {
            List<ComponentConnection> componentConnections = componentConnectionFacade.getComponentConnections(
                CollectionUtils.getFirst(
                    allTasks,
                    curWorkflowTask -> Objects.equals(curWorkflowTask.getName(), workflowTask.getName())));

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            Boolean clusterRoot = componentDefinitionService
                .fetchComponentDefinition(workflowNodeType.name(), workflowNodeType.version())
                .map(ComponentDefinition::isClusterRoot)
                .orElse(Boolean.FALSE);

            workflowTaskDTOs.add(
                new WorkflowTaskDTO(
                    workflowTask, clusterRoot, ClusterElementMap.of(workflowTask.getExtensions()),
                    componentConnections));
        }

        List<WorkflowTriggerDTO> workflowTriggerDTOs = new ArrayList<>();
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTriggerModel : workflowTriggers) {
            workflowTriggerDTOs.add(
                new WorkflowTriggerDTO(
                    workflowTriggerModel,
                    componentConnectionFacade.getComponentConnections(
                        CollectionUtils.getFirst(
                            workflowTriggers,
                            curWorkflowTrigger -> Objects.equals(
                                curWorkflowTrigger.getName(), workflowTriggerModel.getName())))));
        }

        return new WorkflowDTO(workflow, workflowTaskDTOs, workflowTriggerDTOs);
    }
}
