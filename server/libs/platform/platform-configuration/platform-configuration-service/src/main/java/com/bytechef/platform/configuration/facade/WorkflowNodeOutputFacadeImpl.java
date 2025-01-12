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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
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
@Service
@Transactional
public class WorkflowNodeOutputFacadeImpl implements WorkflowNodeOutputFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ActionDefinitionService actionDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowService workflowService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeOutputFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        WorkflowService workflowService, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionDefinitionService = actionDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public WorkflowNodeOutputDTO getWorkflowNodeOutput(String workflowId, String workflowNodeName) {
        WorkflowNodeOutputDTO workflowNodeOutputDTO = null;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (Objects.equals(workflowTrigger.getName(), workflowNodeName)) {
                workflowNodeOutputDTO = getWorkflowNodeOutputDTO(workflowId, workflowTrigger);

                break;
            }
        }

        if (workflowNodeOutputDTO == null) {
            List<WorkflowTask> workflowTasks = workflow.getTasks();

            for (WorkflowTask workflowTask : workflowTasks) {
                if (Objects.equals(workflowTask.getName(), workflowNodeName)) {
                    workflowNodeOutputDTO = getWorkflowNodeOutputDTO(workflowId, workflowTask);
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

            workflowNodeOutputDTOs.add(getWorkflowNodeOutputDTO(workflowId, workflowTrigger));
        }

        List<WorkflowTask> workflowTasks = workflow.getTasks(lastWorkflowNodeName);

        for (WorkflowTask workflowTask : workflowTasks) {
            if (lastWorkflowNodeName != null && Objects.equals(workflowTask.getName(), lastWorkflowNodeName)) {
                break;
            }

            workflowNodeOutputDTOs.add(getWorkflowNodeOutputDTO(workflowId, workflowTask));
        }

        return workflowNodeOutputDTOs;
    }

    @Override
    public Map<String, ?> getPreviousWorkflowNodeSampleOutputs(String workflowId, String lastWorkflowNodeName) {
        return getPreviousWorkflowNodeOutputs(workflowId, lastWorkflowNodeName)
            .stream()
            .filter(workflowNodeOutputDTO -> workflowNodeOutputDTO.sampleOutput() != null)
            .collect(Collectors.toMap(WorkflowNodeOutputDTO::workflowNodeName, WorkflowNodeOutputDTO::sampleOutput));
    }

    @SuppressWarnings("unchecked")
    private OutputResponse checkOutput(
        String workflowId, WorkflowTask workflowTask, WorkflowTrigger workflowTrigger, OutputResponse outputResponse) {

        if (outputResponse != null) {
            // Force UI to test component to get the real fileEntry instance

            if (outputResponse.outputSchema() instanceof FileEntryProperty) {
                return null;
            }

            return outputResponse;
        }

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId);

        if (workflowTask == null) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            Map<String, ?> inputParameters = workflowTrigger.evaluateParameters(inputs);

            Long connectionId = workflowTestConfigurationService
                .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowTrigger.getName())
                .orElse(null);

            outputResponse = triggerDefinitionFacade.executeOutput(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(), inputParameters, connectionId);
        } else {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (workflowNodeType.componentOperationName() == null) {
                return null;
            }

            Map<String, ?> outputs = getPreviousWorkflowNodeSampleOutputs(workflowId, workflowTask.getName());

            Map<String, ?> inputParameters = workflowTask.evaluateParameters(
                MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

            Map<String, Long> connectionIds = MapUtils.toMap(
                workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                    workflowId, workflowTask.getName()),
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId);

            outputResponse = actionDefinitionFacade.executeOutput(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(), inputParameters, connectionIds);
        }

        return outputResponse;
    }

    private OutputResponse checkTriggerOutput(OutputResponse outputResponse, TriggerDefinition triggerDefinition) {
        if (outputResponse != null && !triggerDefinition.isBatch() &&
            outputResponse.outputSchema() instanceof ArrayProperty arrayProperty) {

            List<?> list = arrayProperty.getItems();

            if (!list.isEmpty()) {
                outputResponse = new OutputResponse((BaseProperty) list.getFirst(), outputResponse.sampleOutput());
            }
        }

        return outputResponse;
    }

    private WorkflowNodeOutputDTO getWorkflowNodeOutputDTO(String workflowId, WorkflowTask workflowTask) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ActionDefinition actionDefinition = null;
        TaskDispatcherDefinition taskDispatcherDefinition = null;
        OutputResponse outputResponse;

        if (workflowNodeType.componentOperationName() == null) {
            taskDispatcherDefinition = taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion());

            outputResponse = taskDispatcherDefinition.getOutputResponse();
        } else {
            actionDefinition = actionDefinitionService.getActionDefinition(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName());

            outputResponse = actionDefinition.getOutputResponse();
        }

        OutputResponse finalOutputResponse = outputResponse;

        outputResponse = workflowNodeTestOutputService
            .fetchWorkflowTestNodeOutput(workflowId, workflowTask.getName())
            .map(WorkflowNodeTestOutput::getOutput)
            .orElseGet(() -> checkOutput(workflowId, workflowTask, null, finalOutputResponse));

        return new WorkflowNodeOutputDTO(
            workflowTask.getName(), outputResponse, null, actionDefinition, taskDispatcherDefinition);
    }

    private WorkflowNodeOutputDTO getWorkflowNodeOutputDTO(String workflowId, WorkflowTrigger workflowTrigger) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName());

        OutputResponse outputResponse = workflowNodeTestOutputService
            .fetchWorkflowTestNodeOutput(workflowId, workflowTrigger.getName())
            .map(WorkflowNodeTestOutput::getOutput)
            .orElseGet(() -> checkOutput(workflowId, null, workflowTrigger, triggerDefinition.getOutputResponse()));

        outputResponse = checkTriggerOutput(outputResponse, triggerDefinition);

        return new WorkflowNodeOutputDTO(workflowTrigger.getName(), outputResponse, triggerDefinition, null, null);
    }
}
