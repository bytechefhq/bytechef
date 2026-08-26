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
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
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
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.Property;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.expression.EvaluationException;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final Evaluator evaluator;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowCacheManager workflowCacheManager;
    private final WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade;
    private final WorkflowService workflowService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeOutputFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ActionDefinitionService actionDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        WorkflowCacheManager workflowCacheManager, WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade,
        WorkflowService workflowService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.actionDefinitionService = actionDefinitionService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.evaluator = evaluator;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowCacheManager = workflowCacheManager;
        this.workflowEvaluationInputsFacade = workflowEvaluationInputsFacade;
        this.workflowService = workflowService;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @Nullable
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public ClusterElementOutputDTO getClusterElementOutput(
        String workflowId, String workflowNodeName, String clusterElementType, String clusterElementWorkflowNodeName,
        long environmentId) {

        // See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        ClusterElementOutputDTO clusterElementOutputDTO = null;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTask> workflowTasks = workflow.getTasks(true);

        for (WorkflowTask workflowTask : workflowTasks) {
            if (Objects.equals(workflowTask.getName(), workflowNodeName)) {
                clusterElementOutputDTO = getClusterElementOutputDTO(
                    workflowId, workflowTask, clusterElementType, clusterElementWorkflowNodeName,
                    effectiveEnvironmentId);

                break;
            }
        }

        return clusterElementOutputDTO;
    }

    @Override
    @Nullable
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public WorkflowNodeOutputDTO getWorkflowNodeOutput(String workflowId, String workflowNodeName, long environmentId) {
        // See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        WorkflowNodeOutputDTO workflowNodeOutputDTO = null;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (Objects.equals(workflowTrigger.getName(), workflowNodeName)) {
                workflowNodeOutputDTO = getWorkflowNodeOutputDTO(workflowId, workflowTrigger, effectiveEnvironmentId);

                break;
            }
        }

        if (workflowNodeOutputDTO == null) {
            List<WorkflowTask> workflowTasks = workflow.getTasks(true);

            for (WorkflowTask workflowTask : workflowTasks) {
                if (Objects.equals(workflowTask.getName(), workflowNodeName)) {
                    workflowNodeOutputDTO = getWorkflowNodeOutputDTO(
                        workflowId, workflowTask, null, effectiveEnvironmentId, new HashMap<>());

                    break;
                }
            }
        }

        return workflowNodeOutputDTO;
    }

    // These two methods are also environment-agnostic-gated with an unchecked environmentId (see
    // PrincipalEnvironment), but deliberately do NOT resolve it themselves the way their sibling methods above do.
    // @Cacheable's default key is built from the raw method arguments BEFORE the method body runs, so resolving
    // in here would be too late: the cache would still be keyed on the caller-supplied environmentId, not the
    // effective one, letting a confined principal's PRODUCTION read (requested as DEVELOPMENT) get cached under the
    // DEVELOPMENT key and served back to a genuine DEVELOPMENT caller. Every caller of these two methods must
    // instead resolve BEFORE calling in, so the key is already correct -- WorkflowNodeOutputApiController does this
    // for getPreviousWorkflowNodeOutputs (matching checkWorkflowCache's eviction above), and every other in-module
    // caller (WorkflowNodeScriptFacadeImpl, WorkflowNodeParameterFacadeImpl, WorkflowNodeDynamicPropertiesFacadeImpl,
    // WorkflowNodeTestOutputFacadeImpl, WorkflowNodeOptionFacadeImpl) already resolves its own environmentId before
    // reaching either method. This list is load-bearing, not decorative: a new caller of either method that skips
    // this step reopens the leak silently, since nothing here would catch it.
    @Override
    @Cacheable(value = PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE)
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public List<WorkflowNodeOutputDTO> getPreviousWorkflowNodeOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId) {

        return doGetPreviousWorkflowNodeOutputs(workflowId, lastWorkflowNodeName, environmentId, new HashMap<>());
    }

    @Override
    @Cacheable(value = PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE)
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public Map<String, ?> getPreviousWorkflowNodeSampleOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId) {

        return doGetPreviousWorkflowNodeSampleOutputs(workflowId, lastWorkflowNodeName, environmentId, new HashMap<>());
    }

    @Override
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public void checkWorkflowCache(String workflowId, String lastWorkflowNodeName, long environmentId) {
        // hasPermission(#workflowId, 'Workflow', ...) above is environment-agnostic, so the caller-supplied
        // environmentId is never checked. Resolved here so a confined principal evicts and later reads the same
        // cache entry its own environment owns -- see the WorkflowNodeOutputApiController REST caller, which
        // resolves once and passes the same effective value to this eviction call and to the @Cacheable read that
        // follows it, and the comment on getPreviousWorkflowNodeOutputs below for why the @Cacheable methods
        // themselves must not resolve internally. See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        boolean dynamicOutputDefined = false;
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            if (Objects.equals(workflowTrigger.getName(), lastWorkflowNodeName)) {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                dynamicOutputDefined = triggerDefinitionService.isDynamicOutputDefined(
                    workflowNodeType.name(), workflowNodeType.version(),
                    Objects.requireNonNull(workflowNodeType.operation()));

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
                workflowCacheManager.clearCacheForWorkflow(workflowId, cacheName, effectiveEnvironmentId);
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
                List<?> sampleOutputs = (List<?>) outputResponse.sampleOutput();
                Object firstSampleOutput =
                    sampleOutputs == null || sampleOutputs.isEmpty() ? null : sampleOutputs.getFirst();

                outputResponse = new OutputResponse(
                    (BaseProperty) items.getFirst(), firstSampleOutput, outputResponse.placeholder());
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

    /**
     * Returns the immediate child tasks a dispatcher task can autocomplete against, so
     * {@link #containsWorkflowTask(List, String)} can decide whether the node currently being resolved is nested inside
     * this dispatcher (self-reference) or a sibling of it.
     */
    private static List<WorkflowTask> getChildWorkflowTasks(
        WorkflowTask workflowTask, WorkflowNodeType workflowNodeType) {

        if (Objects.equals(workflowNodeType.name(), "each")) {
            Map<String, ?> iterateeMap = MapUtils.getMap(workflowTask.getParameters(), "iteratee", Map.of());

            return iterateeMap.isEmpty() ? List.of() : List.of(new WorkflowTask(iterateeMap));
        } else if (Objects.equals(workflowNodeType.name(), "condition")) {
            List<WorkflowTask> childWorkflowTasks = new ArrayList<>();

            childWorkflowTasks.addAll(getWorkflowTaskList(workflowTask.getParameters(), "caseTrue"));
            childWorkflowTasks.addAll(getWorkflowTaskList(workflowTask.getParameters(), "caseFalse"));

            return childWorkflowTasks;
        } else if (Objects.equals(workflowNodeType.name(), "branch")) {
            List<WorkflowTask> childWorkflowTasks = new ArrayList<>();

            List<Map<String, ?>> cases = MapUtils.getList(
                workflowTask.getParameters(), "cases", new TypeReference<Map<String, ?>>() {}, List.of());

            for (Map<String, ?> branchCase : cases) {
                childWorkflowTasks.addAll(getWorkflowTaskList(branchCase, "tasks"));
            }

            childWorkflowTasks.addAll(getWorkflowTaskList(workflowTask.getParameters(), "default"));

            return childWorkflowTasks;
        } else if (Objects.equals(workflowNodeType.name(), "fork-join")) {
            List<List<Map<String, ?>>> branches = MapUtils.getList(
                workflowTask.getParameters(), "branches", new TypeReference<>() {}, List.of());

            return branches.stream()
                .flatMap(List::stream)
                .map(WorkflowTask::new)
                .toList();
        } else if (Objects.equals(workflowNodeType.name(), "graph")) {
            return getWorkflowTaskList(workflowTask.getParameters(), "nodes");
        } else {
            return getWorkflowTaskList(workflowTask.getParameters(), "iteratee");
        }
    }

    private static List<WorkflowTask> getWorkflowTaskList(Map<String, ?> parameters, String key) {
        return MapUtils.getList(parameters, key, new TypeReference<Map<String, ?>>() {}, List.of())
            .stream()
            .map(WorkflowTask::new)
            .toList();
    }

    private List<WorkflowNodeOutputDTO> doGetPreviousWorkflowNodeOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId,
        Map<String, Map<String, ?>> sampleOutputsCache) {

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

            if (Objects.equals(workflowNodeType.name(), "loop") || Objects.equals(workflowNodeType.name(), "each") ||
                Objects.equals(workflowNodeType.name(), "map") ||
                Objects.equals(workflowNodeType.name(), "condition") ||
                Objects.equals(workflowNodeType.name(), "branch") ||
                Objects.equals(workflowNodeType.name(), "fork-join") ||
                Objects.equals(workflowNodeType.name(), "graph")) {

                List<WorkflowTask> childWorkflowTasks = getChildWorkflowTasks(workflowTask, workflowNodeType);

                if (containsWorkflowTask(childWorkflowTasks, lastWorkflowNodeName)) {
                    workflowNodeOutputDTOs.add(
                        getWorkflowNodeOutputDTO(workflowId, workflowTask, false, environmentId, sampleOutputsCache));
                } else {
                    workflowNodeOutputDTOs.add(
                        getWorkflowNodeOutputDTO(workflowId, workflowTask, true, environmentId, sampleOutputsCache));
                }
            } else {
                workflowNodeOutputDTOs.add(
                    getWorkflowNodeOutputDTO(workflowId, workflowTask, true, environmentId, sampleOutputsCache));
            }
        }

        return workflowNodeOutputDTOs;
    }

    private Map<String, ?> doGetPreviousWorkflowNodeSampleOutputs(
        String workflowId, String lastWorkflowNodeName, long environmentId,
        Map<String, Map<String, ?>> sampleOutputsCache) {

        Map<String, ?> cached = sampleOutputsCache.get(lastWorkflowNodeName);

        if (cached != null) {
            return cached;
        }

        Map<String, ?> result =
            doGetPreviousWorkflowNodeOutputs(workflowId, lastWorkflowNodeName, environmentId, sampleOutputsCache)
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

        sampleOutputsCache.put(lastWorkflowNodeName, result);

        return result;
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
            Objects.requireNonNull(clusterElementWorkflowNodeType.operation()));

        Class<? extends BaseProperty> typeClass = workflowNodeType.operation() == null
            ? Property.class : com.bytechef.platform.component.domain.Property.class;

        OutputResponse outputResponse = workflowNodeTestOutputService
            .fetchWorkflowTestNodeOutput(workflowId, clusterElementWorkflowNodeName, environmentId)
            .map(workflowNodeTestOutput -> workflowNodeTestOutput.getOutput(typeClass))
            .or(() -> getClusterElementDynamicOutputResponse(workflowId, clusterElement, environmentId))
            .orElse(null);

        if (outputResponse == null) {
            outputResponse = checkOutputSchemaIsFileEntryProperty(clusterElementDefinition.getOutputResponse());
        }

        return new ClusterElementOutputDTO(clusterElementDefinition, outputResponse, clusterElementWorkflowNodeName);
    }

    private Optional<OutputResponse> getClusterElementDynamicOutputResponse(
        String workflowId, ClusterElement clusterElement, long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(clusterElement.getType());

        Map<String, ?> clusterElementParameters = clusterElement.getParameters();

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, clusterElement.getWorkflowNodeName(), environmentId);

        String clusterElementWorkflowNodeName = clusterElement.getWorkflowNodeName();

        Long connectionId = workflowTestConfigurationConnections.stream()
            .filter(connection -> Objects.equals(
                connection.getWorkflowConnectionKey(), clusterElementWorkflowNodeName))
            .map(WorkflowTestConfigurationConnection::getConnectionId)
            .findFirst()
            .orElse(null);

        OutputResponse outputResponse = clusterElementDefinitionFacade.executeOutput(
            clusterElement.getComponentName(), workflowNodeType.version(), workflowNodeType.operation(),
            clusterElementParameters, connectionId);

        return Optional.ofNullable(outputResponse);
    }

    private WorkflowNodeOutputDTO getWorkflowNodeOutputDTO(
        String workflowId, WorkflowTask workflowTask, Boolean taskDispatcherOutput, long environmentId,
        Map<String, Map<String, ?>> sampleOutputsCache) {

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
                        workflowId, workflowTask, taskDispatcherOutput, environmentId, sampleOutputsCache);

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
                    outputResponse = getWorkflowTaskDynamicOutputResponse(
                        workflowId, workflowTask, environmentId, sampleOutputsCache);
                }
            }
        } else {
            // Task dispatchers no longer show variable property outputs (loop, each), only real outputs (like map)
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
            workflowNodeType.name(), workflowNodeType.version(), Objects.requireNonNull(workflowNodeType.operation()));

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
        String workflowId, WorkflowTask workflowTask, long environmentId,
        Map<String, Map<String, ?>> sampleOutputsCache) throws EvaluationException {

        Map<String, ?> inputs = workflowEvaluationInputsFacade.getEvaluationInputs(workflowId, environmentId);
        OutputResponse outputResponse;

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        if (!actionDefinitionService.isDynamicOutputDefined(
            workflowNodeType.name(), workflowNodeType.version(),
            Objects.requireNonNull(workflowNodeType.operation()))) {

            return null;
        }

        Map<String, ?> outputs = doGetPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId, sampleOutputsCache);

        Map<String, ?> inputParameters = null;
        try {
            inputParameters = workflowTask.evaluateParameters(
                MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);
        } catch (RuntimeException e) {
            throw new EvaluationException("Couldn't evaluate expression with sample output", e);
        }

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
        String workflowId, WorkflowTask workflowTask, Boolean taskDispatcherOutput, long environmentId,
        Map<String, Map<String, ?>> sampleOutputsCache) {

        Map<String, ?> inputs = workflowEvaluationInputsFacade.getEvaluationInputs(workflowId, environmentId);
        OutputResponse outputResponse = null;
        OutputResponse variableOutputResponse = null;

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        if (!taskDispatcherDefinitionService.isDynamicOutputDefined(
            workflowNodeType.name(), workflowNodeType.version())) {

            return null;
        }

        Map<String, ?> outputs = doGetPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId, sampleOutputsCache);

        // Leniently: this is the EDITOR's preview of a dispatcher's output, and a property being typed
        // into is a half-written expression most of the time. Evaluating strictly turned every
        // keystroke that did not yet parse into a Bad Request toast over the canvas, for a value
        // nothing was going to run. Execution evaluates strictly, where a broken expression matters.
        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator, true);

        if (taskDispatcherOutput == null || taskDispatcherOutput) {
            Map<String, Object> outputInputParameters = new HashMap<>(inputParameters);

            outputInputParameters.put(MapDataSource.WORKFLOW_ID, workflowId);
            outputInputParameters.put(MapDataSource.ENVIRONMENT_ID, environmentId);

            outputResponse = taskDispatcherDefinitionService.executeOutput(
                workflowNodeType.name(), workflowNodeType.version(), outputInputParameters);

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

        Map<String, ?> inputs = workflowEvaluationInputsFacade.getEvaluationInputs(workflowId, environmentId);
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
