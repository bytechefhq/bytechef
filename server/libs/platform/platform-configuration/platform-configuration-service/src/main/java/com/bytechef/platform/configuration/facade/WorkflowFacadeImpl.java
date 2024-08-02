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

package com.bytechef.platform.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.DataStream;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTaskDTO;
import com.bytechef.platform.configuration.dto.WorkflowTriggerDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowFacadeImpl implements WorkflowFacade {

    private final WorkflowConnectionFacade workflowConnectionFacade;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WorkflowFacadeImpl(WorkflowConnectionFacade workflowConnectionFacade, WorkflowService workflowService) {
        this.workflowConnectionFacade = workflowConnectionFacade;
        this.workflowService = workflowService;
    }

    @Override
    public WorkflowDTO getWorkflow(String id) {
        return toWorkflowDTO(workflowService.getWorkflow(id));
    }

    @Override
    public WorkflowDTO update(String id, String definition, Integer version) {
        return toWorkflowDTO(workflowService.update(id, definition, version));
    }

    private WorkflowDTO toWorkflowDTO(Workflow workflow) {
        List<WorkflowTaskDTO> workflowTaskDTOs = new ArrayList<>();

        for (WorkflowTask workflowTask : workflow.getAllTasks()) {
            workflowTaskDTOs.add(
                new WorkflowTaskDTO(
                    workflowTask,
                    workflowConnectionFacade.getWorkflowConnections(
                        CollectionUtils.getFirst(
                            workflow.getAllTasks(),
                            curWorkflowTask -> Objects.equals(curWorkflowTask.getName(), workflowTask.getName()))),
                    DataStream.of(workflowTask.getExtensions())));
        }

        List<WorkflowTriggerDTO> workflowTriggerDTOs = new ArrayList<>();
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTriggerModel : workflowTriggers) {
            workflowTriggerDTOs.add(
                new WorkflowTriggerDTO(
                    workflowTriggerModel,
                    workflowConnectionFacade.getWorkflowConnections(
                        CollectionUtils.getFirst(
                            workflowTriggers,
                            curWorkflowTrigger -> Objects.equals(
                                curWorkflowTrigger.getName(), workflowTriggerModel.getName())))));
        }

        return new WorkflowDTO(workflow, workflowTaskDTOs, workflowTriggerDTOs);
    }
}
