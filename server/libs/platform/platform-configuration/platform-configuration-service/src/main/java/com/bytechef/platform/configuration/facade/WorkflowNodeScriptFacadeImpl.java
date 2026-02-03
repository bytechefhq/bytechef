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
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.script.CodeEditorScriptInputProvider;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeScriptFacadeImpl implements WorkflowNodeScriptFacade {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowNodeScriptFacadeImpl.class);

    private final List<CodeEditorScriptInputProvider> codeEditorScriptInputProviders;
    private final ConnectionService connectionService;
    private final Evaluator evaluator;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeScriptFacadeImpl(
        List<CodeEditorScriptInputProvider> codeEditorScriptInputProviders, ConnectionService connectionService,
        Evaluator evaluator, WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.codeEditorScriptInputProviders = codeEditorScriptInputProviders;
        this.connectionService = connectionService;
        this.evaluator = evaluator;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowNodeTestOutputFacade = workflowNodeTestOutputFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public Map<String, Object> getClusterElementScriptInput(
        String workflowId, String workflowNodeName, String clusterElementTypeName,
        String clusterElementWorkflowNodeName, long environmentId) {

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

        ClusterElementMap clusterElementMap = ClusterElementMap.of(workflowTask.getExtensions());

        Optional<ClusterElement> sourceClusterElementOptional = clusterElementMap.fetchClusterElement(SOURCE);

        if (sourceClusterElementOptional.isEmpty()) {
            return Map.of();
        }

        ClusterElement sourceClusterElement = sourceClusterElementOptional.get();

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowNodeName, environmentId);

        Map<String, ?> sourceInputParameters = evaluator.evaluate(sourceClusterElement.getParameters(), outputs);

        ComponentConnection sourceComponentConnection = getComponentConnection(
            workflowId, sourceClusterElement.getWorkflowNodeName(), environmentId);

        CodeEditorScriptInputProvider provider = codeEditorScriptInputProviders.stream()
            .filter(inputProvider -> inputProvider.getRootComponentName()
                .equals(workflowNodeType.name()))
            .findFirst()
            .orElse(null);

        if (provider == null) {
            return Map.of();
        }

        try {
            return provider.getScriptInput(
                workflowNodeType.version(), sourceClusterElement.getComponentName(),
                sourceClusterElement.getComponentVersion(), sourceClusterElement.getClusterElementName(),
                sourceInputParameters, sourceComponentConnection);
        } catch (Exception exception) {
            logger.warn("Error getting script input from provider: {}", exception.getMessage(), exception);

            return Map.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWorkflowNodeScriptInput(
        String workflowId, String workflowNodeName, long environmentId) {

        if (logger.isDebugEnabled()) {
            logger.debug(
                "getWorkflowNodeScriptInput called with workflowId={}, workflowNodeName={}, environmentId={}",
                workflowId, workflowNodeName, environmentId);
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        if (logger.isDebugEnabled()) {
            logger.debug("Found workflow task: name={}, type={}", workflowTask.getName(), workflowTask.getType());
        }

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowNodeName, environmentId);

        if (logger.isDebugEnabled()) {
            logger.debug("Context for evaluation - inputs: {}, outputs: {}", inputs, outputs);
        }

        Map<String, ?> evaluatedParameters = workflowTask.evaluateParameters(
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs), evaluator);

        if (logger.isDebugEnabled()) {
            logger.debug("Evaluated parameters: {}", evaluatedParameters);
        }

        Object input = evaluatedParameters.get("input");

        if (logger.isDebugEnabled()) {
            logger.debug(
                "Input parameter: value={}, type={}",
                input, input == null ? "null" : input.getClass()
                    .getName());
        }

        if (input instanceof Map<?, ?> inputMap) {
            return (Map<String, Object>) inputMap;
        }

        return Map.of();
    }

    @Override
    public ScriptTestExecutionDTO testClusterElementScript(
        String workflowId, String workflowNodeName, String clusterElementType,
        String clusterElementWorkflowNodeName, long environmentId) {

        ExecutionError executionError = null;
        WorkflowNodeTestOutput workflowNodeTestOutput = null;

        try {
            workflowNodeTestOutput = workflowNodeTestOutputFacade.saveClusterElementTestOutput(
                workflowId, workflowNodeName, clusterElementType.toUpperCase(), clusterElementWorkflowNodeName,
                environmentId);
        } catch (Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug(exception.getMessage(), exception);
            }

            executionError = extractExecutionError(exception);
        }

        OutputResponse outputResponse = null;

        if (workflowNodeTestOutput != null) {
            outputResponse = workflowNodeTestOutput.getOutput(Property.class);
        }

        return new ScriptTestExecutionDTO(
            executionError, outputResponse == null ? null : outputResponse.sampleOutput());
    }

    @Override
    public ScriptTestExecutionDTO testWorkflowNodeScript(
        String workflowId, String workflowNodeName, long environmentId) {

        ExecutionError executionError = null;
        WorkflowNodeTestOutput workflowNodeTestOutput = null;

        try {
            workflowNodeTestOutput = workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                workflowId, workflowNodeName, environmentId);
        } catch (Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug(exception.getMessage(), exception);
            }

            executionError = extractExecutionError(exception);
        }

        OutputResponse outputResponse = null;

        if (workflowNodeTestOutput != null) {
            outputResponse = workflowNodeTestOutput.getOutput(Property.class);
        }

        return new ScriptTestExecutionDTO(
            executionError, outputResponse == null ? null : outputResponse.sampleOutput());
    }

    private ExecutionError extractExecutionError(Exception exception) {
        Throwable curException = exception;
        String message = exception.getMessage();

        while (curException.getCause() != null) {
            curException = curException.getCause();

            message = curException.getMessage();
        }

        return new ExecutionError(message, Arrays.asList(ExceptionUtils.getStackFrames(exception)));
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
}
