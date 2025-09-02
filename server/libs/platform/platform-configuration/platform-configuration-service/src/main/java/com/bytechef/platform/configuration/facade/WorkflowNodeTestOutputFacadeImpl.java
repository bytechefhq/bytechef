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
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.annotation.WorkflowCacheEvict;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.execution.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    private final Evaluator evaluator;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final TriggerDefinitionFacade triggerDefinitionFacade;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WebhookTriggerTestFacade webhookTriggerTestFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeTestOutputFacadeImpl(
        ActionDefinitionFacade actionDefinitionFacade, Evaluator evaluator,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, WebhookTriggerTestFacade webhookTriggerTestFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.evaluator = evaluator;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.triggerDefinitionFacade = triggerDefinitionFacade;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.webhookTriggerTestFacade = webhookTriggerTestFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @WorkflowCacheEvict(cacheNames = {
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE,
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE
    },
        environmentIdParam = "environmentId",
        workflowIdParam = "workflowId")
    public WorkflowNodeTestOutput saveWorkflowNodeSampleOutput(
        String workflowId, String workflowNodeName, Object sampleOutput, long environmentId) {

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
            workflowId, workflowNodeName, workflowNodeType, new OutputResponse(outputSchema, sampleOutput),
            environmentId);
    }

    @Override
    @WorkflowCacheEvict(cacheNames = {
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE,
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE
    },
        environmentIdParam = "environmentId",
        workflowIdParam = "workflowId")
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Optional<WorkflowTrigger> workflowTrigger = WorkflowTrigger.fetch(workflow, workflowNodeName);

        if (workflowTrigger.isPresent()) {
            Long connectionId = workflowTestConfigurationService
                .fetchWorkflowTestConfigurationConnectionId(workflowId, workflowNodeName, environmentId)
                .orElse(null);

            return saveTriggerWorkflowNodeTestOutput(
                workflowId, workflowNodeName, workflowTrigger.get(), connectionId, environmentId);
        } else {
            List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
                workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                    workflowId, workflowNodeName, environmentId);

            return saveActionWorkflowNodeTestOutput(
                workflowNodeName, workflow,
                MapUtils.toMap(
                    workflowTestConfigurationConnections,
                    WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
                    WorkflowTestConfigurationConnection::getConnectionId),
                environmentId);
        }
    }

    @Override
    public void saveWorkflowNodeTestOutput(
        WorkflowExecutionId workflowExecutionId, long environmentId, WebhookRequest webhookRequest) {

        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        String workflowId = jobPrincipalAccessor.getLatestWorkflowId(
            workflowExecutionId.getWorkflowReferenceCode());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflow)
            .getFirst();

        WorkflowNodeType triggerWorkflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        Map<String, ?> triggerParameters = workflowTrigger.evaluateParameters(
            workflowTestConfigurationService.getWorkflowTestConfigurationInputs(workflowId, environmentId),
            evaluator);

        Long connectionId = null;

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, workflowTrigger.getName(), environmentId);

        if (!workflowTestConfigurationConnections.isEmpty()) {
            WorkflowTestConfigurationConnection workflowTestConfigurationConnection =
                workflowTestConfigurationConnections.getFirst();

            connectionId = workflowTestConfigurationConnection.getConnectionId();
        }

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            triggerWorkflowNodeType.name(), triggerWorkflowNodeType.version(),
            triggerWorkflowNodeType.operation(), workflowExecutionId.getType(), null,
            workflowExecutionId.getWorkflowReferenceCode(), triggerParameters, Map.of(), webhookRequest,
            connectionId, true);

        if (triggerOutput != null && triggerOutput.value() != null) {
            saveWorkflowNodeSampleOutput(
                workflowId, workflowTrigger.getName(), triggerOutput.value(), environmentId);

            webhookTriggerTestFacade.disableTrigger(workflowId, environmentId, workflowExecutionId.getType());
        }
    }

    @SuppressWarnings("unchecked")
    private WorkflowNodeTestOutput saveActionWorkflowNodeTestOutput(
        String workflowNodeName, Workflow workflow, Map<String, Long> connectionIds, long environmentId) {

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflow.getId(), environmentId);
        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflow.getId(), workflowTask.getName(), environmentId);

        Map<String, ?> inputParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);

        Object value = actionDefinitionFacade.executePerform(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), null, null, null, null,
            null, inputParameters, connectionIds, Map.of(), true);

        if (value == null) {
            return null;
        }

        OutputResponse outputResponse = SchemaUtils.toOutput(
            value, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        if (outputResponse == null) {
            return null;
        }

        return workflowNodeTestOutputService.save(
            Validate.notNull(workflow.getId(), "id"), workflowNodeName, workflowNodeType, outputResponse,
            environmentId);
    }

    @SuppressFBWarnings("NP")
    private WorkflowNodeTestOutput saveTriggerWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, WorkflowTrigger workflowTrigger, Long connectionId,
        long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            workflowNodeType.name(), workflowNodeType.version(),
            workflowNodeType.operation(), null, null, null, workflowTrigger.evaluateParameters(inputs, evaluator),
            null, null, connectionId, true);

        Object value = triggerOutput.value();

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> triggerOutputValues) {
            List<Object> outputsList = new ArrayList<>(triggerOutputValues);

            if (!outputsList.isEmpty()) {
                value = outputsList.getFirst();
            }
        }

        OutputResponse outputResponse = SchemaUtils.toOutput(
            value, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        if (outputResponse == null) {
            return null;
        }

        return workflowNodeTestOutputService.save(
            workflowId, workflowNodeName, workflowNodeType, outputResponse, environmentId);
    }
}
