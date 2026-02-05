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

import static com.bytechef.component.definition.datastream.ItemReader.SOURCE;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.configuration.annotation.WorkflowCacheEvict;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeTestOutputFacadeImpl implements WorkflowNodeTestOutputFacade {

    private final ActionDefinitionFacade actionDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final List<CodeEditorScriptInputProvider> codeEditorScriptInputProviders;
    private final ConnectionService connectionService;
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
        ActionDefinitionFacade actionDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        List<CodeEditorScriptInputProvider> codeEditorScriptInputProviders, ConnectionService connectionService,
        Evaluator evaluator, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        TriggerDefinitionFacade triggerDefinitionFacade, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade, WebhookTriggerTestFacade webhookTriggerTestFacade,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.actionDefinitionFacade = actionDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.codeEditorScriptInputProviders = codeEditorScriptInputProviders;
        this.connectionService = connectionService;
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
    })
    public WorkflowNodeTestOutput saveWorkflowNodeSampleOutput(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId, String workflowNodeName, Object sampleOutput,
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {

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
    })
    @SuppressWarnings("unchecked")
    public WorkflowNodeTestOutput saveClusterElementTestOutput(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ClusterElementMap clusterElementMap = ClusterElementMap.of(workflowTask.getExtensions());

        ClusterElementType clusterElementType = clusterElementDefinitionService.getClusterElementType(
            workflowNodeType.name(), workflowNodeType.version(), clusterElementTypeName);

        ClusterElement clusterElement = clusterElementMap.getClusterElement(
            clusterElementType, clusterElementWorkflowNodeName);

        WorkflowNodeType clusterElementWorkflowNodeType = WorkflowNodeType.ofType(clusterElement.getType());

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, clusterElementWorkflowNodeName, environmentId);

        Map<String, Long> connectionIds = MapUtils.toMap(
            workflowTestConfigurationConnections,
            WorkflowTestConfigurationConnection::getWorkflowConnectionKey,
            WorkflowTestConfigurationConnection::getConnectionId);

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowTask.getName(), environmentId);

        Map<String, Object> inputParameters = (Map<String, Object>) workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);

        Map<String, Object> clusterElementInputParameters = evaluator.evaluate(clusterElement.getParameters(), outputs);

        inputParameters = MapUtils.concat(inputParameters, clusterElementInputParameters);

        Map<String, Object> scriptInputParameters = getScriptInputParametersFromSource(
            workflowId, workflowNodeType, clusterElementMap, outputs, environmentId);

        inputParameters = MapUtils.concat(inputParameters, Map.of("input", scriptInputParameters));

        Map<String, ?> extensions = evaluator.evaluate(clusterElement.getExtensions(), outputs);

        return executeAndSaveTestOutput(
            workflow.getId(), clusterElementWorkflowNodeName, clusterElementWorkflowNodeType, inputParameters,
            connectionIds, extensions, environmentId);
    }

    @Override
    @WorkflowCacheEvict(cacheNames = {
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_OUTPUTS_CACHE,
        WorkflowNodeOutputFacade.PREVIOUS_WORKFLOW_NODE_SAMPLE_OUTPUTS_CACHE
    })
    public WorkflowNodeTestOutput saveWorkflowNodeTestOutput(
        @WorkflowCacheEvict.WorkflowIdParam String workflowId, String workflowNodeName,
        @WorkflowCacheEvict.EnvironmentIdParam long environmentId) {

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

        String workflowId = jobPrincipalAccessor.getLastWorkflowId(workflowExecutionId.getWorkflowUuid());

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
            triggerWorkflowNodeType.operation(), null, workflowExecutionId.getWorkflowUuid(), triggerParameters,
            Map.of(), webhookRequest, connectionId, environmentId, workflowExecutionId.getType(), true);

        if (triggerOutput != null && triggerOutput.value() != null) {
            saveWorkflowNodeSampleOutput(
                workflowId, workflowTrigger.getName(), triggerOutput.value(), environmentId);

            webhookTriggerTestFacade.disableTrigger(workflowId, environmentId, workflowExecutionId.getType());
        }
    }

    private WorkflowNodeTestOutput executeAndSaveTestOutput(
        String workflowId, String workflowNodeName, WorkflowNodeType workflowNodeType, Map<String, ?> inputParameters,
        Map<String, Long> connectionIds, Map<String, ?> extensions, long environmentId) {

        Object value = actionDefinitionFacade.executePerform(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), null, null, null, null,
            inputParameters, connectionIds, extensions, environmentId, null, true);

        if (value == null) {
            return null;
        }

        OutputResponse outputResponse = SchemaUtils.toOutput(
            value, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY);

        if (outputResponse == null) {
            return null;
        }

        return workflowNodeTestOutputService.save(
            Validate.notNull(workflowId, "workflowId"), workflowNodeName, workflowNodeType, outputResponse,
            environmentId);
    }

    private ComponentConnection getComponentConnection(
        String workflowId, String clusterElementWorkflowNodeName, long environmentId) {

        List<WorkflowTestConfigurationConnection> connections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, clusterElementWorkflowNodeName, environmentId);

        if (connections.isEmpty()) {
            return null;
        }

        WorkflowTestConfigurationConnection testConnection = connections.getFirst();

        Connection connection = connectionService.getConnection(testConnection.getConnectionId());

        return new ComponentConnection(
            connection.getComponentName(), connection.getConnectionVersion(), connection.getId(),
            connection.getParameters(), connection.getAuthorizationType());
    }

    private Map<String, Object> getScriptInputParametersFromSource(
        String workflowId, WorkflowNodeType workflowNodeType, ClusterElementMap clusterElementMap,
        Map<String, ?> outputs, long environmentId) {

        Optional<ClusterElement> sourceClusterElementOptional = clusterElementMap.fetchClusterElement(SOURCE);

        if (sourceClusterElementOptional.isEmpty()) {
            return Map.of();
        }

        CodeEditorScriptInputProvider codeEditorScriptInputProvider = codeEditorScriptInputProviders.stream()
            .filter(inputProvider -> Objects.equals(inputProvider.getRootComponentName(), workflowNodeType.name()))
            .findFirst()
            .orElse(null);

        if (codeEditorScriptInputProvider == null) {
            return Map.of();
        }

        ClusterElement sourceClusterElement = sourceClusterElementOptional.get();

        Map<String, ?> sourceInputParameters = evaluator.evaluate(sourceClusterElement.getParameters(), outputs);

        ComponentConnection sourceComponentConnection = getComponentConnection(
            workflowId, sourceClusterElement.getWorkflowNodeName(), environmentId);

        return codeEditorScriptInputProvider.getScriptInputParameters(
            workflowNodeType.version(), sourceClusterElement.getComponentName(),
            sourceClusterElement.getComponentVersion(), sourceClusterElement.getClusterElementName(),
            sourceInputParameters, sourceComponentConnection);
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

        Map<String, ?> extensions = evaluator.evaluate(workflowTask.getExtensions(), outputs);

        return executeAndSaveTestOutput(
            workflow.getId(), workflowNodeName, workflowNodeType, inputParameters, connectionIds, extensions,
            environmentId);
    }

    private WorkflowNodeTestOutput saveTriggerWorkflowNodeTestOutput(
        String workflowId, String workflowNodeName, WorkflowTrigger workflowTrigger, Long connectionId,
        long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        TriggerOutput triggerOutput = triggerDefinitionFacade.executeTrigger(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation(), null, null,
            workflowTrigger.evaluateParameters(inputs, evaluator), null, null, connectionId, environmentId, null, true);

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
