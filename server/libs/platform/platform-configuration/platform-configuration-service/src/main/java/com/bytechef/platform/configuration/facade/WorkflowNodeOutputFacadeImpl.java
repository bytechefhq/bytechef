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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.ClusterElementOutputDTO;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkflowNodeOutputFacadeImpl implements WorkflowNodeOutputFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ActionDefinitionService actionDefinitionService;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final Evaluator evaluator;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowService workflowService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeOutputFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        WorkflowCacheManager workflowCacheManager, WorkflowService workflowService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionDefinitionService = actionDefinitionService;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.evaluator = evaluator;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowCacheManager = workflowCacheManager;
        this.workflowService = workflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public ClusterElementOutputDTO getClusterElementOutput(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName,
        long environmentId) {

        ClusterElementOutputDTO clusterElementOutputDTO = null;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTask> workflowTasks = workflow.getTasks(true);

        for (WorkflowTask workflowTask : workflowTasks) {
            if (Objects.equals(workflowTask.getName(), workflowNodeName)) {
                clusterElementOutputDTO = getClusterElementOutputDTO(
                    workflowId, workflowTask, clusterElementType, clusterElementWorkflowNodeName, environmentId);

                break;
            }
        }

        return clusterElementOutputDTO;
    }

    @Override
    public WorkflowNodeOutputDTO getWorkflowNodeOutput(String workflowId, String workflowNodeName, long environmentId) {
        WorkflowNodeOutputDTO workflowNodeOutputDTO = null;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (Objects.equals(workflowTrigger.getName(), workflowNodeName)) {
                workflowNodeOutputDTO = getWorkflowNodeOutputDTO(workflowId, workflowTrigger, environmentId);

                break;
            }
        }

        if (workflowNodeOutputDTO == null) {
            List<WorkflowTask> workflowTasks = workflow.getTasks(true);

            for (WorkflowTask workflowTask : workflowTasks) {
                if (Objects.equals(workflowTask.getName(), workflowNodeName)) {
                    workflowNodeOutputDTO = getWorkflowNodeOutputDTO(workflowId, workflowTask, null, environmentId);

                    break;
                }
            }
        }

        return workflowNodeOutputDTO;
    }

    @Override
    @Cacheable(value = PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE)
    public List<WorkflowNodeOutputDTO> getPreviousWorkflowNodeOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId) {

        return doGetPreviousWorkflowNodeOutputs(workflowId, lastWorkflowNodeName, environmentId);
    }

    @Override
    @Cacheable(value = PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE)
    public Map<String, ?> getPreviousWorkflowNodeSampleOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId) {

        return doGetPreviousWorkflowNodeSampleOutputs(workflowId, lastWorkflowNodeName, environmentId);
    }

    @Override
    public void checkWorkflowCache(String workflowId, String lastWorkflowNodeName, long environmentId) {
        boolean dynamicOutputDefined = false;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (Objects.equals(workflowTrigger.getName(), lastWorkflowNodeName)) {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                dynamicOutputDefined = triggerDefinitionService.isDynamicOutputDefined(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

                break;
            }
        }

        if (!dynamicOutputDefined) {
            List<WorkflowTask> workflowTasks = workflow.getTasks(lastWorkflowNodeName);

            for (WorkflowTask workflowTask : workflowTasks) {
                if (Objects.equals(workflowTask.getName(), lastWorkflowNodeName)) {
                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    if (workflowNodeType.operation() == null) {
                        dynamicOutputDefined = taskDispatcherDefinitionService.isDynamicOutputDefined(
                            workflowNodeType.name(), workflowNodeType.version());
                    } else {
                        dynamicOutputDefined = actionDefinitionService.isDynamicOutputDefined(
                            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());
                    }

                    break;
                }
            }
        }

        if (dynamicOutputDefined) {
            for (String cacheName : WORKFLOW_CACHE_NAMES) {
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName, environmentId);
            }
        }
    }

    private OutputResponse checkOutputSchemaIsFileEntryProperty(OutputResponse outputResponse) {
        if (outputResponse != null && outputResponse.outputSchema() instanceof FileEntryProperty) {
            // Force UI to test component to get the real fileEntry instance

            return null;
        }

        return outputResponse;
    }

    private OutputResponse checkTriggerOutput(OutputResponse outputResponse, TriggerDefinition triggerDefinition) {
        if (outputResponse != null && !triggerDefinition.isBatch() &&
            outputResponse.outputSchema() instanceof ArrayProperty arrayProperty) {

            List<?> items = arrayProperty.getItems();

            if (!items.isEmpty()) {
                List<?> sampleOutput = (List<?>) outputResponse.sampleOutput();

                outputResponse = new OutputResponse(
                    (BaseProperty) items.getFirst(), sampleOutput.getFirst(), outputResponse.placeholder());
            }
        }

        return outputResponse;
    }

    private static boolean containsWorkflowTask(List<WorkflowTask> workflowTasks, String workflowNodeName) {
        List<WorkflowTask> allWorkflowTasks = workflowTasks.stream()
            .flatMap(workflowTask -> CollectionUtils.stream(workflowTask.getTasks()))
            .toList();

        return allWorkflowTasks.stream()
            .anyMatch(workflowTask -> Objects.equals(workflowTask.getName(), workflowNodeName));
    }

    private List<WorkflowNodeOutputDTO> doGetPreviousWorkflowNodeOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId) {

        List<WorkflowNodeOutputDTO> workflowNodeOutputDTOs = new ArrayList<>();

        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (lastWorkflowNodeName != null && Objects.equals(workflowTrigger.getName(), lastWorkflowNodeName)) {
                break;
            }

            workflowNodeOutputDTOs.add(getWorkflowNodeOutputDTO(workflowId, workflowTrigger, environmentId));
        }

        List<WorkflowTask> workflowTasks = workflow.getTasks(lastWorkflowNodeName);

        for (WorkflowTask workflowTask : workflowTasks) {
            if (lastWorkflowNodeName != null && Objects.equals(workflowTask.getName(), lastWorkflowNodeName)) {
                break;
            }

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (Objects.equals(workflowNodeType.name(), "loop")) {
                List<WorkflowTask> childWorkflowTasks = MapUtils
                    .getList(
                        workflowTask.getParameters(), "iteratee", new TypeReference<Map<String, ?>>() {}, List.of())
                    .stream()
                    .map(WorkflowTask::new)
                    .toList();

                if (containsWorkflowTask(childWorkflowTasks, lastWorkflowNodeName)) {
                    workflowNodeOutputDTOs.add(
                        getWorkflowNodeOutputDTO(workflowId, workflowTask, false, environmentId));
                }
            } else {
                workflowNodeOutputDTOs.add(getWorkflowNodeOutputDTO(workflowId, workflowTask, true, environmentId));
            }
        }

        return workflowNodeOutputDTOs;
    }

    private Map<String, ?> doGetPreviousWorkflowNodeSampleOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId) {

        return doGetPreviousWorkflowNodeOutputs(workflowId, lastWorkflowNodeName, environmentId)
            .stream()
            .filter(workflowNodeOutputDTO -> workflowNodeOutputDTO.getSampleOutput() != null ||
                workflowNodeOutputDTO.getVariableSampleOutput() != null)
            .collect(
                Collectors.toMap(
                    WorkflowNodeOutputDTO::workflowNodeName,
                    workflowNodeOutputDTO -> {
                        if (workflowNodeOutputDTO.getSampleOutput() != null) {
                            return workflowNodeOutputDTO.getSampleOutput();
                        }

                        return workflowNodeOutputDTO.getVariableSampleOutput();
                    }));
    }

    private ClusterElementOutputDTO getClusterElementOutputDTO(
        String workflowId, WorkflowTask workflowTask, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ClusterElementMap clusterElementMap = ClusterElementMap.of(workflowTask.getExtensions());

        com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType clusterElementType =
            clusterElementDefinitionService.getClusterElementType(
                workflowNodeType.name(), workflowNodeType.version(), clusterElementTypeName);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(
            clusterElementType, clusterElementWorkflowNodeName);

        WorkflowNodeType clusterElementWorkflowNodeType = WorkflowNodeType.ofType(clusterElement.getType());

        ClusterElementDefinition clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
            clusterElementWorkflowNodeType.name(), clusterElementWorkflowNodeType.version(),
            clusterElementWorkflowNodeType.operation());

        Class<? extends BaseProperty> typeClass = workflowNodeType.operation() == null
            ? Property.class : com.bytechef.platform.component.domain.Property.class;

        OutputResponse outputResponse = workflowNodeTestOutputService
            .fetchWorkflowTestNodeOutput(workflowId, clusterElementWorkflowNodeName, environmentId)
            .map(workflowNodeTestOutput -> workflowNodeTestOutput.getOutput(typeClass))
            .or(() -> getClusterElementDynamicOutputResponse(workflowId, workflowTask, clusterElement, environmentId))
            .orElse(null);

        if (outputResponse == null) {
            outputResponse = checkOutputSchemaIsFileEntryProperty(clusterElementDefinition.getOutputResponse());
        }

        return new ClusterElementOutputDTO(clusterElementDefinition, outputResponse, clusterElementWorkflowNodeName);
    }

    @SuppressWarnings("unchecked")
    private Optional<OutputResponse> getClusterElementDynamicOutputResponse(
        String workflowId, WorkflowTask workflowTask, ClusterElement clusterElement, long environmentId) {

        OutputResponse outputResponse;
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(clusterElement.getType());

        Map<String, ?> outputs = doGetPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId);

        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, clusterElement.getWorkflowNodeName(), environmentId);

        Map<String, Long> connectionIds = MapUtils.toMap(
            workflowTestConfigurationConnections,
            WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
            WorkflowTestConfigurationConnection::getConnectionId);

        // Fix, cluster element tools are not necessarily the same as actions
        outputResponse = actionDefinitionFacade.executeOutput(
            clusterElement.getComponentName(), workflowNodeType.version(), workflowNodeType.operation(),
            inputParameters, connectionIds);

        return Optional.ofNullable(outputResponse);
    }

    private WorkflowNodeOutputDTO getWorkflowNodeOutputDTO(
        String workflowId, WorkflowTask workflowTask, Boolean taskDispatcherOutput, long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ActionDefinition actionDefinition = null;
        TaskDispatcherDefinition taskDispatcherDefinition = null;
        OutputResponse variableOutputResponse = null;
        boolean testoutputResponse = false;

        Class<? extends BaseProperty> typeClass = workflowNodeType.operation() == null
            ? Property.class : com.bytechef.platform.component.domain.Property.class;

        OutputResponse outputResponse = workflowNodeTestOutputService
            .fetchWorkflowTestNodeOutput(workflowId, workflowTask.getName(), environmentId)
            .map(workflowNodeTestOutput -> workflowNodeTestOutput.getOutput(typeClass))
            .orElse(null);

        if (workflowNodeType.operation() == null) {
            taskDispatcherDefinition = taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                workflowNodeType.name(), workflowNodeType.version());
        } else {
            actionDefinition = actionDefinitionService.getActionDefinition(
                workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());
        }

        if (outputResponse == null) {
            if (workflowNodeType.operation() == null) {
                WorkflowTaskDispatcherDynamicOutputResponse workflowTaskDispatcherDynamicOutputResponse =
                    getWorkflowTaskDispatcherDynamicOutputResponse(
                        workflowId, workflowTask, taskDispatcherOutput, environmentId);

                if (workflowTaskDispatcherDynamicOutputResponse == null) {
                    outputResponse = taskDispatcherDefinition.getOutputResponse();
                } else {
                    outputResponse = checkOutputSchemaIsFileEntryProperty(
                        workflowTaskDispatcherDynamicOutputResponse.outputResponse);
                    variableOutputResponse = workflowTaskDispatcherDynamicOutputResponse.variableOutputResponse;
                }
            } else {
                outputResponse = checkOutputSchemaIsFileEntryProperty(actionDefinition.getOutputResponse());

                if (outputResponse == null) {
                    outputResponse = getWorkflowTaskDynamicOutputResponse(workflowId, workflowTask, environmentId);
                }
            }
        } else {
            // Currently, variable output samples are stored as the output response in db
            if (taskDispatcherOutput == null || !taskDispatcherOutput) {
                variableOutputResponse = outputResponse;

                outputResponse = null;
            }

            testoutputResponse = true;
        }

        return new WorkflowNodeOutputDTO(
            actionDefinition, null, outputResponse, taskDispatcherDefinition, testoutputResponse, null,
            variableOutputResponse, workflowTask.getName());
    }

    private WorkflowNodeOutputDTO getWorkflowNodeOutputDTO(
        String workflowId, WorkflowTrigger workflowTrigger, long environmentId) {

        boolean testoutputResponse = false;
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        Class<? extends BaseProperty> typeClass = workflowNodeType.operation() == null
            ? Property.class : com.bytechef.platform.component.domain.Property.class;
        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowNodeType.name(), workflowNodeType.version(),
            workflowNodeType.operation());

        OutputResponse outputResponse = workflowNodeTestOutputService
            .fetchWorkflowTestNodeOutput(workflowId, workflowTrigger.getName(), environmentId)
            .map(workflowNodeTestOutput -> workflowNodeTestOutput.getOutput(typeClass))
            .or(() -> getWorkflowTriggerDynamicOutputResponse(workflowId, workflowTrigger, environmentId))
            .orElse(null);

        if (outputResponse == null) {
            outputResponse = checkOutputSchemaIsFileEntryProperty(triggerDefinition.getOutputResponse());
        } else {
            testoutputResponse = true;
        }

        outputResponse = checkTriggerOutput(outputResponse, triggerDefinition);

        return new WorkflowNodeOutputDTO(
            null, null, outputResponse, null, testoutputResponse, triggerDefinition, workflowTrigger.getName());
    }

    @SuppressWarnings("unchecked")
    private OutputResponse getWorkflowTaskDynamicOutputResponse(
        String workflowId, WorkflowTask workflowTask, long environmentId) {

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        OutputResponse outputResponse;

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        if (!actionDefinitionService.isDynamicOutputDefined(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation())) {

            return null;
        }

        Map<String, ?> outputs = doGetPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId);

        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService
                .getWorkflowTestConfigurationConnections(workflowId, workflowTask.getName(), environmentId);

        Map<String, Long> connectionIds = MapUtils.toMap(
            workflowTestConfigurationConnections,
            WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
            WorkflowTestConfigurationConnection::getConnectionId);

        outputResponse = actionDefinitionFacade.executeOutput(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), inputParameters,
            connectionIds);

        return outputResponse;
    }

    @SuppressWarnings("unchecked")
    private WorkflowTaskDispatcherDynamicOutputResponse getWorkflowTaskDispatcherDynamicOutputResponse(
        String workflowId, WorkflowTask workflowTask, Boolean taskDispatcherOutput, long environmentId) {

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        OutputResponse outputResponse = null;
        OutputResponse variableOutputResponse = null;

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        if (!taskDispatcherDefinitionService.isDynamicOutputDefined(
            workflowNodeType.name(), workflowNodeType.version())) {

            return null;
        }

        Map<String, ?> outputs = doGetPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId);

        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);

        if (taskDispatcherOutput == null || taskDispatcherOutput) {
            outputResponse = taskDispatcherDefinitionService.executeOutput(
                workflowNodeType.name(), workflowNodeType.version(), inputParameters);

            if (outputResponse != null && outputResponse.outputSchema() instanceof ObjectProperty objectProperty) {
                List<? extends Property> properties = objectProperty.getProperties();

                if (properties.isEmpty()) {
                    outputResponse = null;
                }
            }
        }

        if (taskDispatcherOutput == null || !taskDispatcherOutput) {
            variableOutputResponse = taskDispatcherDefinitionService.executeVariableProperties(
                workflowNodeType.name(), workflowNodeType.version(), inputParameters);

            if (variableOutputResponse != null &&
                variableOutputResponse.outputSchema() instanceof ObjectProperty objectProperty) {

                List<? extends Property> properties = objectProperty.getProperties();

                if (properties.isEmpty()) {
                    variableOutputResponse = null;
                }
            }
        }

        return new WorkflowTaskDispatcherDynamicOutputResponse(outputResponse, variableOutputResponse);
    }

    private Optional<OutputResponse> getWorkflowTriggerDynamicOutputResponse(
        String workflowId, WorkflowTrigger workflowTrigger, long environmentId) {

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        Map<String, ?> inputParameters = workflowTrigger.evaluateParameters(inputs, evaluator);

        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowTrigger.getName(), environmentId)
            .orElse(null);

        return Optional.ofNullable(
            triggerDefinitionFacade.executeOutput(
                workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), inputParameters,
                connectionId));
    }

    private record WorkflowTaskDispatcherDynamicOutputResponse(
        OutputResponse outputResponse, OutputResponse variableOutputResponse) {
    }
}
