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
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeOptionFacadeImpl implements WorkflowNodeOptionFacade {

    private final Evaluator evaluator;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeOptionFacadeImpl(
        Evaluator evaluator, ActionDefinitionFacade actionDefinitionFacade,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.evaluator = evaluator;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Option> getClusterElementNodeOptions(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        @Nullable String searchText, long environmentId) {

        List<WorkflowTestConfigurationConnection> connections = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, environmentId)
            .stream()
            .flatMap(workflowTestConfiguration -> CollectionUtils.stream(
                workflowTestConfiguration.getConnections()))
            .toList();

        Map<String, Long> clusterElementConnectionIds = connections.stream()
            .collect(Collectors.toMap(
                WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                WorkflowTestConfigurationConnection::getConnectionId));

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId);

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

        Map<String, Map<String, ?>> result = new java.util.HashMap<>();

        for (Map.Entry<String, Object> entry : clusterElementMap.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof ClusterElement clusterElement) {
                result.put(
                    clusterElement.getWorkflowNodeName(), evaluator.evaluate(clusterElement.getParameters(), context));
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof ClusterElement clusterElement) {
                        result.put(
                            clusterElement.getWorkflowNodeName(),
                            evaluator.evaluate(clusterElement.getParameters(), context));
                    }
                }
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Option> getWorkflowNodeOptions(
        String workflowId, String workflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        @Nullable String searchText, long environmentId) {

        Long connectionId = workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowNodeName, environmentId)
            .orElse(null);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger
            .fetch(workflow, workflowNodeName)
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                return triggerDefinitionFacade.executeOptions(
                    workflowNodeType.name(), workflowNodeType.version(),
                    workflowNodeType.operation(), propertyName, workflowTrigger.evaluateParameters(inputs, evaluator),
                    lookupDependsOnPaths, searchText, connectionId);
            })
            .orElseGet(
                () -> {
                    WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                    Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                        workflowId, workflowTask.getName(), environmentId);
                    WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                    return actionDefinitionFacade.executeOptions(
                        workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), propertyName,
                        workflowTask.evaluateParameters(
                            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator),
                        lookupDependsOnPaths, searchText, connectionId);
                });
    }
}
