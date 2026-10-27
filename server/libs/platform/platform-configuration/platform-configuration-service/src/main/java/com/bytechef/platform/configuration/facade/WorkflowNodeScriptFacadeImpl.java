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
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.CodeEditorScriptInputFacade;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.bytechef.platform.configuration.domain.WorkflowNodeTestOutput;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.dto.ScriptTestExecutionDTO;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.OutputResponse;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowNodeScriptFacadeImpl implements WorkflowNodeScriptFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNodeScriptFacadeImpl.class);

    private final CodeEditorScriptInputFacade codeEditorScriptInputFacade;
    private final Evaluator evaluator;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public WorkflowNodeScriptFacadeImpl(
        CodeEditorScriptInputFacade codeEditorScriptInputFacade, Evaluator evaluator,
        WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.codeEditorScriptInputFacade = codeEditorScriptInputFacade;
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

        Long sourceConnectionId = getSourceConnectionId(
            workflowId, workflowNodeName, sourceClusterElement.getWorkflowNodeName(), environmentId);

        try {
            return codeEditorScriptInputFacade.getScriptInputParameters(
                workflowNodeType.name(), workflowNodeType.version(), sourceClusterElement.getComponentName(),
                sourceClusterElement.getComponentVersion(), sourceClusterElement.getClusterElementName(),
                sourceInputParameters, sourceConnectionId);
        } catch (Exception exception) {
            log.warn("Error getting script input from provider: {}", exception.getMessage(), exception);

            return Map.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWorkflowNodeScriptInput(
        String workflowId, String workflowNodeName, long environmentId) {

        if (log.isDebugEnabled()) {
            log.debug(
                "getWorkflowNodeScriptInput called with workflowId={}, workflowNodeName={}, environmentId={}",
                workflowId, workflowNodeName, environmentId);
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask workflowTask = workflow.getTask(workflowNodeName);

        if (log.isDebugEnabled()) {
            log.debug("Found workflow task: name={}, type={}", workflowTask.getName(), workflowTask.getType());
        }

        Map<String, ?> parameters = workflowTask.getParameters();

        Object input = parameters.get("input");

        if (!(input instanceof Map<?, ?> inputMap)) {
            return Map.of();
        }

        Map<String, ?> inputs = workflowTestConfigurationService.getWorkflowTestConfigurationInputs(
            workflowId, environmentId);

        Map<String, ?> outputs = workflowNodeOutputFacade.getPreviousWorkflowNodeSampleOutputs(
            workflowId, workflowNodeName, environmentId);

        if (log.isDebugEnabled()) {
            log.debug("Context for evaluation - inputs: {}, outputs: {}", inputs, outputs);
        }

        Map<String, Object> evaluatedInput = evaluator.evaluate(
            (Map<String, Object>) inputMap,
            MapUtils.concat((Map<String, Object>) inputs, (Map<String, Object>) outputs));

        if (log.isDebugEnabled()) {
            log.debug("Evaluated input: {}", evaluatedInput);
        }

        return evaluatedInput;
    }

    @Override
    public ScriptTestExecutionDTO testClusterElementScript(
        String workflowId, String workflowNodeName, String clusterElementType,
        String clusterElementWorkflowNodeName, long environmentId, Map<String, Object> inputParameters) {

        return executeTestAndBuildResult(() -> {
            if (inputParameters == null) {
                return workflowNodeTestOutputFacade.saveClusterElementTestOutput(
                    workflowId, workflowNodeName, clusterElementType.toUpperCase(Locale.ROOT),
                    clusterElementWorkflowNodeName, environmentId);
            }

            return workflowNodeTestOutputFacade.saveClusterElementTestOutput(
                workflowId, workflowNodeName, clusterElementType.toUpperCase(Locale.ROOT),
                clusterElementWorkflowNodeName, Map.of("input", inputParameters), environmentId);
        });
    }

    @Override
    public ScriptTestExecutionDTO testWorkflowNodeScript(
        String workflowId, String workflowNodeName, long environmentId, Map<String, Object> inputParameters) {

        return executeTestAndBuildResult(() -> {
            if (inputParameters == null) {
                return workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                    workflowId, workflowNodeName, environmentId);
            }

            return workflowNodeTestOutputFacade.saveWorkflowNodeTestOutput(
                workflowId, workflowNodeName, Map.of("input", inputParameters), environmentId);
        });
    }

    private ScriptTestExecutionDTO executeTestAndBuildResult(Supplier<WorkflowNodeTestOutput> testOutputSupplier) {
        ExecutionError executionError = null;
        WorkflowNodeTestOutput workflowNodeTestOutput = null;

        try {
            workflowNodeTestOutput = testOutputSupplier.get();
        } catch (Exception exception) {
            log.warn(exception.getMessage(), exception);

            executionError = extractExecutionError(exception);
        }

        OutputResponse outputResponse = null;

        if (workflowNodeTestOutput != null) {
            outputResponse = workflowNodeTestOutput.getOutput(Property.class);
        }

        return new ScriptTestExecutionDTO(executionError, getOutputAsMap(outputResponse));
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOutputAsMap(OutputResponse outputResponse) {
        if (outputResponse == null) {
            return null;
        }

        Object sampleOutput = outputResponse.sampleOutput();

        if (sampleOutput == null) {
            return null;
        }

        if (sampleOutput instanceof Map) {
            return (Map<String, Object>) sampleOutput;
        }

        return Map.of("value", sampleOutput);
    }

    private @Nullable Long getSourceConnectionId(
        String workflowId, String workflowNodeName, String sourceWorkflowNodeName, long environmentId) {

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationService.getWorkflowTestConfigurationConnections(
                workflowId, workflowNodeName, environmentId);

        return workflowTestConfigurationConnections.stream()
            .filter(workflowTestConfigurationConnection -> Objects.equals(
                workflowTestConfigurationConnection.getWorkflowConnectionKey(), sourceWorkflowNodeName))
            .map(WorkflowTestConfigurationConnection::getConnectionId)
            .findFirst()
            .orElse(null);
    }
}
