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
import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.NullProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.registry.domain.OutputResponse;
import com.bytechef.platform.registry.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeTestOutputFacadeImpl implements WorkflowNodeTestOutputFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, TriggerDefinitionFacade triggerDefinitionFacade,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(String workflowId, String workflowNodeName) {
        Workflow workflow = workflowService.getWorkflow(workflowId);

        Optional<WorkflowTrigger> workflowTrigger = WorkflowTrigger.fetch(workflow, workflowNodeName);

        if (workflowTrigger.isPresent()) {
            return saveTriggerWorkflowNodeTestOutput(
                workflowId, workflowNodeName, workflowTrigger.get(),
                workflowTestConfigurationService
                    .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowNodeName)
                    .orElse(null));
        } else {
            return saveActionWorkflowNodeTestOutput(
                workflowNodeName, workflow,
                MapUtils.toMap(
                    workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                        workflowId, workflowNodeName),
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId));
        }
    }

    @Override
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, Object sampleOutput) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        String type = WorkflowTrigger.fetch(workflow, workflowNodeName)
            .map(WorkflowTrigger::getType)
            .orElseGet(() -> {
                WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

                return workflowTask.getType();
            });

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(type);

        Property outputSchema = Property.toProperty(
            (com.bytechef.component.definition.Property) SchemaUtils.getOutputSchema(
                sampleOutput, PropertyFactory.PROPERTY_FACTORY));

        return workflowNodeTestOutputService.save(
            workflowId, workflowNodeName, workflowNodeType, new OutputResponse(outputSchema, sampleOutput));
    }

    @SuppressWarnings("unchecked")
    private WorkflowNodeTestOutput saveActionWorkflowNodeTestOutput(
        String workflowNodeName, Workflow workflow, Map<String, Long> connectionIds) {

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflow.getId());
        Map<String, ?> outputs = workflowNodeOutputFacade.getWorkflowNodeSampleOutputs(
            workflow.getId(), workflowTask.getName());

        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

        Object object = actionDefinitionFacade.executePerform(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), null, null, null, null, null, inputParameters,
            connectionIds, Map.of(), true);

        OutputResponse outputResponse =
            SchemaUtils.toOutput(
                new BaseOutputDefinition.OutputResponse(
                    (BaseProperty.BaseValueProperty<?>) SchemaUtils.getOutputSchema(
                        object, PropertyFactory.PROPERTY_FACTORY),
                    object),
                (property, sampleOutput) -> new OutputResponse(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput),
                PropertyFactory.PROPERTY_FACTORY);

        if (outputResponse == null || outputResponse.outputSchema() instanceof NullProperty) {
            return null;
        }

        return workflowNodeTestOutputService.save(
            Validate.notNull(workflow.getId(), "id"), workflowNodeName, workflowNodeType, outputResponse);
    }

    @SuppressFBWarnings("NP")
    private WorkflowNodeTestOutput saveTriggerWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, WorkflowTrigger workflowTrigger, Long connectionId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId);

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            workflowNodeType.componentName(), workflowNodeType.componentVersion(),
            workflowNodeType.componentOperationName(), null, null, null, workflowTrigger.evaluateParameters(inputs),
            null, null, connectionId, true);

        OutputResponse outputResponse =
            SchemaUtils.toOutput(
                new BaseOutputDefinition.OutputResponse(
                    (BaseProperty.BaseValueProperty<?>) SchemaUtils.getOutputSchema(
                        triggerOutput.value(), PropertyFactory.PROPERTY_FACTORY),
                    triggerOutput.batch()),
                (property, sampleOutput) -> new OutputResponse(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput),
                PropertyFactory.PROPERTY_FACTORY);

        if (outputResponse == null) {
            return null;
        }

        return workflowNodeTestOutputService.save(workflowId, workflowNodeName, workflowNodeType, outputResponse);
    }
}
