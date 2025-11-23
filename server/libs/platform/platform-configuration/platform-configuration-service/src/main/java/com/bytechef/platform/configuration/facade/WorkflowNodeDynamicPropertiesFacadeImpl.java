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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeDynamicPropertiesFacadeImpl implements WorkflowNodeDynamicPropertiesFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final Evaluator evaluator;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowService workflowService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeDynamicPropertiesFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, Evaluator evaluator,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.evaluator = evaluator;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowService = workflowService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Property> getClusterElementDynamicProperties(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        long environmentId) {

        Long connectionId = getConnectionId(workflowId, workflowNodeName, environmentId);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowNodeType.name(), environmentId);

        ClusterElementMap clusterElementMap = ClusterElementMap.of(workflowTask.getExtensions());

        ClusterElement clusterElement = clusterElementMap.getClusterElement(
            clusterElementDefinitionService.getClusterElementType(
                workflowNodeType.name(), workflowNodeType.version(), clusterElementTypeName),
            clusterElementWorkflowNodeName);

        WorkflowNodeType clusterElementWorkflowNodeType = WorkflowNodeType.ofType(clusterElement.getType());

        return clusterElementDefinitionFacade.executeDynamicProperties(
            clusterElementWorkflowNodeType.name(), clusterElementWorkflowNodeType.version(),
            clusterElementWorkflowNodeType.operation(), propertyName,
            evaluator.evaluate(
                clusterElement.getParameters(),
                MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs)),
            lookupDependsOnPaths, connectionId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Property> getWorkflowNodeDynamicProperties(
        String workflowId, String workflowNodeName, String propertyName, List<String> lookupDependsOnPaths,
        long environmentId) {

        Long connectionId = getConnectionId(workflowId, workflowNodeName, environmentId);
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger
            .fetch(workflow, workflowNodeName)
            .map(workflowTrigger -> {
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                return triggerDefinitionFacade.executeDynamicProperties(
                    workflowNodeType.name(), workflowNodeType.version(),
                    workflowNodeType.operation(), propertyName, workflowTrigger.evaluateParameters(inputs, evaluator),
                    lookupDependsOnPaths, connectionId);
            })
            .orElseGet(() -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
                    workflowId, workflowTask.getName(), environmentId);
                WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

                return actionDefinitionFacade.executeDynamicProperties(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), propertyName,
                    workflowTask.evaluateParameters(
                        MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator),
                    lookupDependsOnPaths, workflowId, connectionId);
            });
    }

    private Long getConnectionId(String workflowId, String workflowNodeName, long environmentId) {
        return workflowTestConfigurationService
            .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowNodeName, environmentId)
            .orElse(null);
    }
}
