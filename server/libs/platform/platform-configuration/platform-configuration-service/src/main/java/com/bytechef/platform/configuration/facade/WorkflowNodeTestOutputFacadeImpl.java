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
import com.bytechef.platform.component.registry.domain.ActionDefinition;
import com.bytechef.platform.component.registry.domain.Output;
import com.bytechef.platform.component.registry.domain.TriggerDefinition;
import com.bytechef.platform.component.registry.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.registry.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.registry.service.ActionDefinitionService;
import com.bytechef.platform.component.registry.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.Type;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeTestOutputFacadeImpl implements WorkflowNodeTestOutputFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputFacadeImpl(
        ActionDefinitionService actionDefinitionService, ActionDefinitionFacade actionDefinitionFacade,
        TriggerDefinitionFacade triggerDefinitionFacade, TriggerDefinitionService triggerDefinitionService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionService = actionDefinitionService;
        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.triggerDefinitionService = triggerDefinitionService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(String workflowId, String workflowNodeName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        return WorkflowTrigger.fetch(workflow, workflowNodeName)
            .map(workflowTrigger -> executeTriggerWorkflowNodeTestOutput(
                workflowId, workflowNodeName, workflowTrigger,
                workflowTestConfigurationService
                    .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowNodeName)
                    .orElse(null)))
            .orElseGet(() -> saveActionWorkflowNodeTestOutput(
                workflowId, workflowNodeName, workflow,
                MapUtils.toMap(
                    workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                        workflowId, workflowNodeName),
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId)));
    }

    @SuppressWarnings("unchecked")
    private WorkflowNodeTestOutput saveActionWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, Workflow workflow, Map<String, Long> connectionIds) {

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName());
        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId);
        Map<String, ?> outputs = workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName());

        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

        if (actionDefinition.isOutputFunctionDefined()) {
            Output output = actionDefinitionFacade.executeOutput(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(), inputParameters, connectionIds);

            if (output == null) {
                return null;
            }

            return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, output);
        } else {
            Object sampleOutput = actionDefinitionFacade.executePerform(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(), Type.AUTOMATION, null, workflowId, null, inputParameters,
                connectionIds);

            if (sampleOutput == null) {
                return null;
            }

            return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, sampleOutput);
        }
    }

    @SuppressFBWarnings("NP")
    private WorkflowNodeTestOutput executeTriggerWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, WorkflowTrigger workflowTrigger, Long connectionId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName());

        if (triggerDefinition.isOutputFunctionDefined()) {
            Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId);

            Output output = triggerDefinitionFacade.executeOutput(
                workflowNodeType.componentName(), workflowNodeType.componentVersion(),
                workflowNodeType.componentOperationName(), workflowTrigger.evaluateParameters(inputs), connectionId);

            return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, output);
        } else {
            // TODO
            Map<String, ?> result = null;

            return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, result);
        }
    }

    @Override
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, Object sampleOutput) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, sampleOutput);
    }
}
