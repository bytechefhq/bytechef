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
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.security.web.authentication.PrincipalEnvironment;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * Both methods require {@code WORKFLOW_VIEW} rather than {@code WORKFLOW_EDIT}, and the choice is forced rather than
 * merely defensible: most of this class was ALREADY transitively gated at {@code WORKFLOW_VIEW} on this same
 * {@code workflowId}, through {@code WorkflowNodeOutputFacadeImpl#getPreviousWorkflowNodeSampleOutputs}. Gating the
 * entry point at {@code WORKFLOW_EDIT} would have made one facade demand two different scopes for one request, and a
 * caller holding {@code WORKFLOW_EDIT} without {@code WORKFLOW_VIEW} would pass the outer gate and then 403 on the
 * inner one.
 *
 * <p>
 * That is also what bounds the genuinely new denial surface, which is the fact worth having before touching this: it is
 * exactly two branches, both in {@code getWorkflowNodeOptions}. The trigger branch never reaches
 * {@code WorkflowNodeOutputFacade} at all, and the task-dispatcher branch
 * ({@code workflowNodeType.operation() == null}) returns before it does. Everything else --
 * {@code getClusterElementNodeOptions} in full, and the action branch -- already answered to a {@code WORKFLOW_VIEW}
 * check, just later than it should have: after the test-configuration connection ids and the {@code vars} merge had
 * already been read.
 *
 * <p>
 * Independently, {@code WORKFLOW_VIEW} is what this package uses for a method of this shape.
 * {@link WorkflowNodeDynamicPropertiesFacadeImpl} is structurally the same method -- same test-configuration connection
 * resolution, same evaluation inputs, same sample outputs, and the same kind of live outbound call through a resolved
 * {@code connectionId} ({@code executeDynamicProperties} in place of {@code executeOptions}) -- and carries
 * {@code WORKFLOW_VIEW}. The discriminator across this package is mutation of stored state, not whether a call leaves
 * the process: {@code WorkflowNodeScriptFacadeImpl} splits {@code getWorkflowNodeScriptInput} ({@code WORKFLOW_VIEW})
 * from {@code testWorkflowNodeScript} ({@code WORKFLOW_EDIT}), and {@code WorkflowTestConfigurationFacadeImpl} splits
 * its reads from its writes the same way. Neither method here writes anything.
 *
 * <p>
 * {@code WORKFLOW_EDIT} would also deny a legitimate caller: {@code WORKFLOW_VIEW} is granted from {@code VIEWER} and
 * {@code WORKFLOW_EDIT} only from {@code EDITOR}, so a workspace viewer opening a workflow read-only would get empty
 * dropdowns and 403s beside a dynamic-properties call that still succeeds -- for no gain, since the outbound call is
 * already reachable at {@code WORKFLOW_VIEW} through that adjacent method.
 *
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeOptionFacadeImpl implements WorkflowNodeOptionFacade {

    private final Evaluator evaluator;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;
    private final WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeOptionFacadeImpl(
        Evaluator evaluator, ActionDefinitionFacade actionDefinitionFacade,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowService workflowService,
        WorkflowEvaluationInputsFacade workflowEvaluationInputsFacade,
        WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.evaluator = evaluator;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.workflowEvaluationInputsFacade = workflowEvaluationInputsFacade;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public List<Option> getClusterElementNodeOptions(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        @Nullable String searchText, long environmentId) {

        // hasPermission(#workflowId, 'Workflow', ...) above is environment-agnostic, so the caller-supplied
        // environmentId is never checked by it. The value reaching getEvaluationInputs (the `vars` merge point), the
        // @Cacheable getPreviousWorkflowNodeSampleOutputs, and the test-configuration connection lookups below must
        // still be the caller's own environment, not an arbitrary one it names -- executeOptions ultimately makes a
        // live outbound call using whatever connectionId those lookups resolve. See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        List<WorkflowTestConfigurationConnection> connections = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, effectiveEnvironmentId)
            .stream()
            .flatMap(workflowTestConfiguration -> CollectionUtils.stream(
                workflowTestConfiguration.getConnections()))
            .toList();

        Map<String, Long> clusterElementConnectionIds = connections.stream()
            .collect(Collectors.toMap(
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId,
                (existing, ignored) -> existing));

        Map<String, ?> inputs = workflowEvaluationInputsFacade.getEvaluationInputs(workflowId, effectiveEnvironmentId);
        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), effectiveEnvironmentId);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ClusterElementType clusterElementType = clusterElementDefinitionService.getClusterElementType(
            workflowNodeType.name(), workflowNodeType.version(), clusterElementTypeName);

        ClusterElementMap clusterElementMap = ClusterElementMap.of(workflowTask.getExtensions());

        ClusterElement clusterElement = clusterElementMap.getClusterElement(
            clusterElementType, clusterElementWorkflowNodeName);

        Map<String, Object> context = MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs);

        Map<String, Map<String, ?>> clusterElementInputParameters = evaluateClusterElementInputParameters(
            clusterElementMap, context);

        WorkflowNodeType clusterElementWorkflowNodeType = WorkflowNodeType.ofType(clusterElement.getType());

        Long connectionId = connections.stream()
            .filter(workflowTestConfigurationConnection -> Objects.equals(
                workflowTestConfigurationConnection.getWorkflowConnectionKey(), clusterElementWorkflowNodeName))
            .findFirst()
            .map(WorkflowTestConfigurationConnection::getConnectionId)
            .orElse(null);

        return clusterElementDefinitionFacade.executeOptions(
            clusterElementWorkflowNodeType.name(), clusterElementWorkflowNodeType.version(),
            clusterElementWorkflowNodeType.operation(), propertyName,
            evaluator.evaluate(clusterElement.getParameters(), context), workflowTask.getExtensions(),
            lookupDependsOnPaths, searchText, connectionId, clusterElementConnectionIds,
            clusterElementInputParameters);
    }

    private Map<String, Map<String, ?>> evaluateClusterElementInputParameters(
        ClusterElementMap clusterElementMap, Map<String, Object> context) {

        Map<String, Map<String, ?>> parameterMap = new java.util.HashMap<>();

        for (Map.Entry<String, Object> entry : clusterElementMap.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof ClusterElement clusterElement) {
                parameterMap.put(
                    clusterElement.getWorkflowNodeName(), evaluator.evaluate(clusterElement.getParameters(), context));

                addNestedClusterElementParameters(clusterElement, context, parameterMap);
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof ClusterElement clusterElement) {
                        parameterMap.put(
                            clusterElement.getWorkflowNodeName(),
                            evaluator.evaluate(clusterElement.getParameters(), context));

                        addNestedClusterElementParameters(clusterElement, context, parameterMap);
                    }
                }
            }
        }

        return parameterMap;
    }

    private void addNestedClusterElementParameters(
        ClusterElement clusterElement, Map<String, Object> context, Map<String, Map<String, ?>> parameterMap) {

        Map<String, ?> extensions = clusterElement.getExtensions();

        if (extensions == null || !extensions.containsKey(WorkflowExtConstants.CLUSTER_ELEMENTS)) {
            return;
        }

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        parameterMap.putAll(evaluateClusterElementInputParameters(clusterElementMap, context));
    }

    @Override
    @SuppressWarnings("unchecked")
    @PreAuthorize("hasPermission(#workflowId, 'Workflow', 'WORKFLOW_VIEW')")
    public List<Option> getWorkflowNodeOptions(
        String workflowId, String workflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        @Nullable String searchText, long environmentId) {

        // Same as getClusterElementNodeOptions above: the gate is environment-agnostic, so the environmentId reaching
        // evaluation, cache, and connection lookups below must still be resolved to the caller's own. The trigger
        // branch below never reaches WorkflowNodeOutputFacade, so before this gate existed it ran entirely ungated.
        // See PrincipalEnvironment.
        long effectiveEnvironmentId = PrincipalEnvironment.resolveEffectiveEnvironmentId(environmentId);

        Map<String, ?> inputs = workflowEvaluationInputsFacade.getEvaluationInputs(workflowId, effectiveEnvironmentId);
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger
            .fetch(workflow, workflowNodeName)
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                Long connectionId = workflowTestConfigurationService
                    .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowNodeName, effectiveEnvironmentId)
                    .orElse(null);

                return triggerDefinitionFacade.executeOptions(
                    workflowNodeType.name(), workflowNodeType.version(),
                    workflowNodeType.operation(), propertyName, workflowTrigger.evaluateParameters(inputs, evaluator),
                    lookupDependsOnPaths, searchText, connectionId);
            })
            .orElseGet(
                () -> {
                    WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    if (workflowNodeType.operation() == null) {
                        return taskDispatcherDefinitionService.executeOptions(
                            workflowNodeType.name(), workflowNodeType.version(), propertyName, searchText)
                            .stream()
                            .map(Option::new)
                            .toList();
                    }

                    Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                        workflowId, workflowTask.getName(), effectiveEnvironmentId);

                    Map<String, Long> connectionIds = workflowTestConfigurationService
                        .fetchWorkflowTestConfiguration(workflowId, effectiveEnvironmentId)
                        .stream()
                        .flatMap(workflowTestConfiguration -> CollectionUtils.stream(
                            workflowTestConfiguration.getConnections()))
                        .filter(connection -> Objects.equals(
                            connection.getWorkflowNodeName(), workflowNodeName))
                        .collect(Collectors.toMap(
                            WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                            WorkflowTestConfigurationConnection::getConnectionId,
                            (existing, ignored) -> existing));

                    return actionDefinitionFacade.executeOptions(
                        workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), propertyName,
                        workflowTask.evaluateParameters(
                            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator),
                        lookupDependsOnPaths, searchText, connectionIds, workflowTask.getExtensions());
                });
    }
}
