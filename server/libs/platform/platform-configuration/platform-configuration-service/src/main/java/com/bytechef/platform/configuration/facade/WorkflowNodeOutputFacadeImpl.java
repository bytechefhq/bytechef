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
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.FileEntryProperty;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
// TODO add support for task dispatchers
@Service
@Transactional
public class WorkflowNodeOutputFacadeImpl implements WorkflowNodeOutputFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowService workflowService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeOutputFacadeImpl(
        ActionDefinitionService actionDefinitionService, TriggerDefinitionService triggerDefinitionService,
        WorkflowService workflowService, WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.actionDefinitionService = actionDefinitionService;
        this.workflowService = workflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public WorkflowNodeOutputDTO getWorkflowNodeOutput(String workflowId, String workflowNodeName) {
        WorkflowNodeOutputDTO workflowNodeOutputDTO = null;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (Objects.equals(workflowTrigger.getName(), workflowNodeName)) {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                    workflowNodeType.componentOperationName());

                Output output = workflowNodeTestOutputService
                    .fetchWorkflowTestNodeOutput(workflowId, workflowTrigger.getName())
                    .map(WorkflowNodeTestOutput::getOutput)
                    .orElseGet(() -> checkOutput(triggerDefinition.getOutput()));

                workflowNodeOutputDTO = new WorkflowNodeOutputDTO(
                    null, output, null, triggerDefinition, workflowTrigger.getName());

                break;
            }
        }

        if (workflowNodeOutputDTO == null) {
            List<WorkflowTask> workflowTasks = workflow.getTasks();

            for (WorkflowTask workflowTask : workflowTasks) {
                if (Objects.equals(workflowTask.getName(), workflowNodeName)) {
                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                        workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                        workflowNodeType.componentOperationName());

                    Output output = workflowNodeTestOutputService
                        .fetchWorkflowTestNodeOutput(workflowId, workflowTask.getName())
                        .map(WorkflowNodeTestOutput::getOutput)
                        .orElseGet(() -> checkOutput(actionDefinition.getOutput()));

                    workflowNodeOutputDTO = new WorkflowNodeOutputDTO(
                        actionDefinition, output, null, null, workflowTask.getName());
                }
            }
        }

        return workflowNodeOutputDTO;
    }

    @Override
    public List<WorkflowNodeOutputDTO> getPreviousWorkflowNodeOutputs(String workflowId, String lastWorkflowNodeName) {
        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = new ArrayList<>();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (lastWorkflowNodeName != null && Objects.equals(workflowTrigger.getName(), lastWorkflowNodeName)) {
                break;
            }

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName());

            Output output = workflowNodeTestOutputService
                .fetchWorkflowTestNodeOutput(workflowId, workflowTrigger.getName())
                .map(WorkflowNodeTestOutput::getOutput)
                .orElseGet(() -> checkOutput(triggerDefinition.getOutput()));

            workflowNodeOutputDTOs.add(
                new WorkflowNodeOutputDTO(null, output, null, triggerDefinition, workflowTrigger.getName()));
        }

        List<WorkflowTask> workflowTasks = workflow.getTasks();

        for (WorkflowTask workflowTask : workflowTasks) {
            if (lastWorkflowNodeName != null && Objects.equals(workflowTask.getName(), lastWorkflowNodeName)) {
                break;
            }

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName());

            Output output = workflowNodeTestOutputService
                .fetchWorkflowTestNodeOutput(workflowId, workflowTask.getName())
                .map(WorkflowNodeTestOutput::getOutput)
                .orElseGet(() -> checkOutput(actionDefinition.getOutput()));

            workflowNodeOutputDTOs.add(
                new WorkflowNodeOutputDTO(actionDefinition, output, null, null, workflowTask.getName()));
        }

        return workflowNodeOutputDTOs;
    }

    @Override
    public Map<String, ?> getWorkflowNodeSampleOutputs(String workflowId, String lastWorkflowNodeName) {
        return getPreviousWorkflowNodeOutputs(workflowId, lastWorkflowNodeName)
            .stream()
            .filter(workflowNodeOutputDTO -> workflowNodeOutputDTO.sampleOutput() != null)
            .collect(Collectors.toMap(WorkflowNodeOutputDTO::workflowNodeName, WorkflowNodeOutputDTO::sampleOutput));
    }

    private static Output checkOutput(Output output) {
        // Force UI to test component to get real fileEntry instance

        if (output != null && output.getOutputSchema() instanceof FileEntryProperty) {
            return null;
        }

        return output;
    }
}
