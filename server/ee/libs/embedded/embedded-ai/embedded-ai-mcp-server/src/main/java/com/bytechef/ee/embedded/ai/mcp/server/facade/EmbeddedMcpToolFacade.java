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
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
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
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
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
        McpComponentService mcpComponentService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
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
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.principalJobFacade = principalJobFacade;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
    }

    public @Nullable FunctionToolCallback<Map<String, Object>, Object> getFunctionToolCallback(
        McpTool mcpTool, String externalUserId, Environment environment, String tenantId) {

        Long integrationInstanceId = fetchIntegrationInstanceId(
            externalUserId, mcpTool.getMcpComponentId(), environment);

        if (integrationInstanceId != null && !isToolEnabled(integrationInstanceId, mcpTool.getId())) {
            return null;
        }

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
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration, String externalUserId,
        Environment environment, String tenantId) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfiguration.getId())) {

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                continue;
            }

            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

            Integration integration = integrationService.getIntegration(
                integrationInstanceConfiguration.getIntegrationId());

            Long integrationInstanceId = fetchIntegrationInstanceId(
                externalUserId, integration.getComponentName(), environment);

            if (integrationInstanceId != null &&
                !isWorkflowEnabled(integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflow.getId())) {

                continue;
            }

            Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

            WorkflowTrigger trigger = getMcpToolCallableTrigger(workflow);

            if (trigger == null) {
                continue;
            }

            Map<String, ?> workflowParameters = mcpIntegrationInstanceConfigurationWorkflow.getParameters();

            String toolName = MapUtils.getString(workflowParameters, ToolConstants.TOOL_NAME);

            if (toolName == null) {
                toolName = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_NAME);
            }

            List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

            FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
                .builder(
                    Objects.requireNonNull(toolName),
                    getWorkflowToolCallbackFunction(
                        externalUserId, integration.getComponentName(), integration.getId(),
                        integrationInstanceConfigurationWorkflow,
                        trigger.getName(), workflowParameters, mcpIntegrationInstanceConfiguration.getMcpServerId(),
                        environment, tenantId))
                .inputType(Map.class)
                .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

            String description = MapUtils.getString(workflowParameters, ToolConstants.TOOL_DESCRIPTION);

            if (description == null) {
                description = MapUtils.getString(trigger.getParameters(), ToolConstants.TOOL_DESCRIPTION);
            }

            if (description != null) {
                builder.description(description);
            }

            toolCallbacks.add(builder.build());
        }

        return toolCallbacks;
    }

    private @Nullable Long fetchIntegrationInstanceId(
        String externalUserId, long mcpComponentId, Environment environment) {

        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpComponentId);

        return fetchIntegrationInstanceId(externalUserId, mcpComponent.getComponentName(), environment);
    }

    private @Nullable Long fetchConnectionId(String externalUserId, String componentName, Environment environment) {
        return connectedUserService.fetchConnectedUser(externalUserId, environment)
            .map(ConnectedUser::getId)
            .flatMap(connectedUserId -> integrationInstanceService.fetchIntegrationInstance(
                connectedUserId, componentName, environment))
            .map(IntegrationInstance::getConnectionId)
            .orElse(null);
    }

    private @Nullable Long fetchIntegrationInstanceId(
        String externalUserId, String componentName, Environment environment) {

        return connectedUserService.fetchConnectedUser(externalUserId, environment)
            .map(ConnectedUser::getId)
            .flatMap(connectedUserId -> integrationInstanceService.fetchIntegrationInstance(
                connectedUserId, componentName, environment))
            .map(IntegrationInstance::getId)
            .orElse(null);
    }

    private Function<Map<String, Object>, Object> getClusterElementToolCallbackFunction(
        String externalUserId, String componentName, int componentVersion, String clusterElementName,
        Map<String, ?> parameters, long mcpServerId, Environment environment, String tenantId) {

        return request -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new IllegalStateException("MCP server is disabled");
            }

            Long connectionId = fetchConnectionId(externalUserId, componentName, environment);

            if (connectionId == null) {
                long integrationId = getIntegrationId(componentName);

                return getConnectionRequiredResponse(
                    componentName, environment, externalUserId, integrationId, tenantId);
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

    private long getIntegrationId(String componentName) {
        return integrationService.getIntegrations()
            .stream()
            .filter(integration -> componentName.equals(integration.getComponentName()))
            .map(Integration::getId)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No integration found for component: " + componentName));
    }

    private Function<Map<String, Object>, Object> getWorkflowToolCallbackFunction(
        String externalUserId, String componentName, long integrationId,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String triggerName,
        Map<String, ?> workflowParameters, long mcpServerId, Environment environment, String tenantId) {

        return inputParameters -> {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            if (!mcpServer.isEnabled()) {
                throw new IllegalStateException("MCP server is disabled");
            }

            Long integrationInstanceId = fetchIntegrationInstanceId(externalUserId, componentName, environment);

            if (integrationInstanceId == null) {
                return getConnectionRequiredResponse(
                    componentName, environment, externalUserId, integrationId, tenantId);
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
                    jobParameters, integrationInstanceId, PlatformType.EMBEDDED),
                true, taskExecutionCompleteEvent -> {});

            if (job.getOutputs() == null) {
                return null;
            }

            return getCallableResponseOutput(job)
                .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs()));
        };
    }

    private boolean isToolEnabled(long integrationInstanceId, long mcpToolId) {
        return mcpIntegrationInstanceToolService
            .fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId)
            .map(McpIntegrationInstanceTool::isEnabled)
            .orElse(false);
    }

    private boolean isWorkflowEnabled(long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId) {
        return mcpIntegrationInstanceConfigurationWorkflowService
            .fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceConfigurationWorkflowId)
            .map(mcpConfigWorkflow -> integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(
                    integrationInstanceId, mcpConfigWorkflow.getIntegrationInstanceConfigurationWorkflowId())
                .map(IntegrationInstanceWorkflow::isEnabled)
                .orElse(false))
            .orElse(false);
    }

    private Map<String, Object> getConnectionRequiredResponse(
        String componentName, Environment environment, String externalUserId, long integrationId, String tenantId) {

        String jwtToken = connectTokenService.generateJwtToken(
            environment.ordinal(), externalUserId, integrationId, tenantId);

        String setupUrl = publicUrl + "/connect.html?token=" + jwtToken;

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
        } catch (IllegalArgumentException | ClassCastException exception) {
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
