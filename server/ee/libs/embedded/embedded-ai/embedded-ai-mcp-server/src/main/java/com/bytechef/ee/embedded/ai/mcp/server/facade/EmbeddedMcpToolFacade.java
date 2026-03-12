/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import com.bytechef.ai.tool.FromAiResult;
import com.bytechef.ai.tool.constant.ToolConstants;
import com.bytechef.ai.tool.facade.AbstractToolFacade;
import com.bytechef.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.ee.embedded.ai.mcp.server.service.ConnectTokenService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/**
 * Tool facade for the embedded MCP server. Handles both component-level tools (direct action execution) and integration
 * workflow tools (workflow-based execution via JobSyncExecutor). Connection resolution is per-connected-user via
 * {@link IntegrationInstanceService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpToolFacade extends AbstractToolFacade {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedMcpToolFacade.class);

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectTokenService connectTokenService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final JobSyncExecutor jobSyncExecutor;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationWorkflowService mcpIntegrationWorkflowService;
    private final McpServerService mcpServerService;
    private final PrincipalJobFacade principalJobFacade;
    private final String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpToolFacade(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, ConnectedUserService connectedUserService,
        ConnectTokenService connectTokenService, Evaluator evaluator,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        JobSyncExecutor jobSyncExecutor,
        McpComponentService mcpComponentService, McpIntegrationWorkflowService mcpIntegrationWorkflowService,
        McpServerService mcpServerService, PrincipalJobFacade principalJobFacade, String publicUrl,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService) {

        super(evaluator);

        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.connectedUserService = connectedUserService;
        this.connectTokenService = connectTokenService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.jobSyncExecutor = jobSyncExecutor;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationWorkflowService = mcpIntegrationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.principalJobFacade = principalJobFacade;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(
        McpTool mcpTool, String externalUserId, Environment environment, String tenantId) {

        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

        List<FromAiResult> fromAiResults = extractFromAiResults(mcpTool.getParameters());

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
            .builder(
                getToolName(
                    clusterElementDefinition.getComponentName(), clusterElementDefinition.getName(),
                    mcpTool.getParameters()),
                getClusterElementToolCallbackFunction(
                    externalUserId, clusterElementDefinition.getComponentName(),
                    clusterElementDefinition.getComponentVersion(),
                    clusterElementDefinition.getName(), mcpTool.getParameters(),
                    mcpComponent.getMcpServerId(), environment, tenantId))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        if (clusterElementDefinition.getDescription() != null) {
            builder.description(clusterElementDefinition.getDescription());
        }

        return builder.build();
    }

    public List<ToolCallback> getFunctionToolCallbacks(
        McpIntegration mcpIntegration, String externalUserId, Environment environment, String tenantId) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpIntegrationWorkflow mcpIntegrationWorkflow : mcpIntegrationWorkflowService
            .getMcpIntegrationMcpIntegrationWorkflows(mcpIntegration.getId())) {

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                continue;
            }

            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

            Integration integration = integrationService.getIntegration(
                integrationInstanceConfiguration.getIntegrationId());

            Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

            WorkflowTrigger trigger = getMcpToolCallableTrigger(workflow);

            if (trigger == null) {
                continue;
            }

            String toolName = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_NAME);
            Map<String, ?> workflowParameters = mcpIntegrationWorkflow.getParameters();

            List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
                .builder(
                    Objects.requireNonNull(toolName),
                    getWorkflowToolCallbackFunction(
                        externalUserId, integration.getComponentName(), integrationInstanceConfigurationWorkflow,
                        trigger.getName(), workflowParameters, mcpIntegration.getMcpServerId(), environment, tenantId))
                .inputType(Map.class)
                .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

            String description = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_DESCRIPTION);

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private Function<Map<String, Object>, Object> getClusterElementToolCallbackFunction(
        String externalUserId, String componentName, int componentVersion, String clusterElementName,
        Map<String, ?> parameters, long mcpServerId, Environment environment, String tenantId) {

        return request -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new IllegalStateException("MCP server is disabled");
            }

            Long connectionId = resolveConnectionId(externalUserId, componentName, environment);

            if (connectionId == null) {
                return getConnectionRequiredResponse(componentName, externalUserId, mcpServerId, tenantId);
            }

            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            return clusterElementDefinitionFacade.executeTool(
                componentName, componentVersion, clusterElementName, MapUtils.concat(request, resolvedParameters),
                connectionId);
        };
    }

    private Function<Map<String, Object>, Object> getWorkflowToolCallbackFunction(
        String externalUserId, String componentName,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String triggerName,
        Map<String, ?> workflowParameters, long mcpServerId, Environment environment, String tenantId) {

        return inputParameters -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new IllegalStateException("MCP server is disabled");
            }

            Long connectionId = resolveConnectionId(externalUserId, componentName, environment);

            if (connectionId == null) {
                return getConnectionRequiredResponse(componentName, externalUserId, mcpServerId, tenantId);
            }

            Map<String, Object> inputs = new HashMap<>(integrationInstanceConfigurationWorkflow.getInputs());

            Map<String, Object> triggerInputs = new HashMap<>();

            for (Map.Entry<String, ?> entry : workflowParameters.entrySet()) {
                triggerInputs.put(entry.getKey(), resolveParameterValue(entry.getValue(), inputParameters));
            }

            triggerInputs.putAll(inputParameters);

            inputs.put(triggerName, triggerInputs);

            Job job = jobSyncExecutor.execute(
                new JobParametersDTO(integrationInstanceConfigurationWorkflow.getWorkflowId(), inputs),
                jobParameters -> principalJobFacade.createSyncJob(
                    jobParameters, integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                    PlatformType.EMBEDDED),
                true, taskExecutionCompleteEvent -> {});

            if (job.getOutputs() == null) {
                return null;
            }

            return getCallableResponseOutput(job)
                .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
        };
    }

    private @Nullable Long resolveConnectionId(String externalUserId, String componentName, Environment environment) {
        try {
            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                externalUserId, environment);

            return integrationInstanceService
                .fetchIntegrationInstance(connectedUser.getId(), componentName, environment)
                .map(IntegrationInstance::getConnectionId)
                .orElse(null);
        } catch (Exception exception) {
            logger.debug("Could not resolve connection for user '{}' and component '{}': {}",
                externalUserId, componentName, exception.getMessage());

            return null;
        }
    }

    private Map<String, Object> getConnectionRequiredResponse(
        String componentName, String externalUserId, long mcpServerId, String tenantId) {

        String token = connectTokenService.generateToken(mcpServerId, componentName, externalUserId, tenantId);

        String setupUrl = publicUrl + "/connect.html?token=" + token;

        return Map.of(
            "error", "connection_required",
            "message",
            "The " + componentName + " integration is not connected for this user. " +
                "To connect, visit: " + setupUrl + " . " +
                "Instruct the user to visit this link to connect their account.",
            "setupUrl", setupUrl);
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        try {
            return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
                .filter(
                    lastTaskExecution -> {
                        Map<String, ?> metadata = lastTaskExecution.getMetadata();

                        return metadata.containsKey(MetadataConstants.CALLABLE_RESPONSE);
                    })
                .map(lastTaskExecution -> {
                    ActionDefinition.CallableResponse callableResponse = ConvertUtils.convertValue(
                        taskFileStorage.readTaskExecutionOutput(lastTaskExecution.getOutput()),
                        ActionDefinition.CallableResponse.class);

                    return callableResponse.output();
                });
        } catch (Exception exception) {
            logger.warn(
                "Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }

    private static @Nullable WorkflowTrigger getMcpToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            String name = workflowNodeType.name();
            String operation = workflowNodeType.operation();

            if (Objects.equals(name, WorkflowConstants.WORKFLOW) &&
                Objects.equals(operation, WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
