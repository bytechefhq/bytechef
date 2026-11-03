/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static com.bytechef.task.dispatcher.approval.constant.WaitForApprovalTaskDispatcherConstants.WAIT_FOR_APPROVAL;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationComponent;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserMcpAutomationResolver;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.constant.EmbeddedToolConstants;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.ai.tool.FromAiResult;
import com.bytechef.platform.ai.tool.ToolOptionsResolver;
import com.bytechef.platform.ai.tool.facade.AbstractToolFacade;
import com.bytechef.platform.ai.tool.util.FromAiInputSchemaUtils;
import com.bytechef.platform.ai.tool.util.ToolNameUtils;
import com.bytechef.platform.ai.tool.util.ToolParameterDefaultsUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationRecorder;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import com.bytechef.platform.external.invocation.WorkflowToolRunResult;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobExecutionErrors;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedMcpToolFacade extends AbstractToolFacade {
    private static final Logger log = LoggerFactory.getLogger(EmbeddedMcpToolFacade.class);

    private static final String APPROVAL = "approval";
    private static final Set<String> APPROVAL_COMPONENT_NAMES = Set.of(APPROVAL, WAIT_FOR_APPROVAL);
    private static final String APPROVAL_REQUIRED_STATUS = "approval_required";
    private static final String STOPPED_MESSAGE = "The workflow run was stopped before it finished.";

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ToolOptionsResolver toolOptionsResolver;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver;
    private final ConnectedUserService connectedUserService;
    private final JwtTokenService jwtTokenService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;
    private final McpComponentService mcpComponentService;
    private final McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;
    private final McpIntegrationComponentService mcpIntegrationComponentService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpServerService mcpServerService;
    private final PrincipalJobFacade principalJobFacade;
    private final String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final ExternalInvocationRecorder externalInvocationRecorder;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpToolFacade(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService,
        ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver,
        ConnectedUserService connectedUserService, Evaluator evaluator,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        JobCompletionAwaiter jobCompletionAwaiter, JwtTokenService jwtTokenService,
        McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService,
        McpComponentService mcpComponentService,
        McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService,
        McpIntegrationComponentService mcpIntegrationComponentService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService,
        McpServerService mcpServerService, PrincipalJobFacade principalJobFacade, String publicUrl,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage,
        ExternalInvocationRecorder externalInvocationRecorder, WorkflowService workflowService) {

        super(evaluator);

        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.toolOptionsResolver = new ToolOptionsResolver(clusterElementDefinitionFacade);
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserMcpAutomationResolver = connectedUserMcpAutomationResolver;
        this.connectedUserService = connectedUserService;
        this.jwtTokenService = jwtTokenService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.mcpAutomationProjectConfigurationWorkflowService = mcpAutomationProjectConfigurationWorkflowService;
        this.mcpComponentService = mcpComponentService;
        this.mcpConnectedUserProjectWorkflowService = mcpConnectedUserProjectWorkflowService;
        this.mcpIntegrationComponentService = mcpIntegrationComponentService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceWorkflowService = mcpIntegrationInstanceWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.principalJobFacade = principalJobFacade;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.externalInvocationRecorder = externalInvocationRecorder;
        this.workflowService = workflowService;
    }

    public @Nullable ToolCallback getFunctionToolCallback(
        McpTool mcpTool, @Nullable String externalUserId, Environment environment, String tenantId,
        @Nullable Authentication authentication) {

        if (!mcpTool.isEnabled()) {
            return null;
        }

        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        Long integrationId = fetchMcpComponentIntegrationId(mcpComponent);

        IntegrationInstance integrationInstance = fetchIntegrationInstance(
            externalUserId, mcpComponent.getComponentName(), integrationId, environment);

        if (integrationInstance != null && !isToolEnabled(integrationInstance.getId(), mcpTool.getId())) {
            return null;
        }

        ClusterElementDefinition clusterElementDefinition =
            clusterElementDefinitionService.getClusterElementDefinition(
                mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

        Long connectionId = integrationInstance == null ? null : integrationInstance.getConnectionId();

        List<FromAiResult> fromAiResults = applyRuntimeOptions(
            extractFromAiResults(mcpTool.getParameters()), mcpTool.getParameters(),
            clusterElementDefinition.getProperties(),
            property -> connectionId == null
                ? null
                : toolOptionsResolver.resolveOptions(
                    clusterElementDefinition.getComponentName(), clusterElementDefinition.getComponentVersion(),
                    clusterElementDefinition.getName(), property, mcpTool.getParameters(), connectionId));

        String toolName = getToolName(
            clusterElementDefinition.getComponentName(), clusterElementDefinition.getName(), mcpTool.getParameters());

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
            .builder(
                toolName,
                withCallerContext(
                    getClusterElementToolCallbackFunction(
                        toolName, externalUserId, clusterElementDefinition.getComponentName(),
                        clusterElementDefinition.getComponentVersion(),
                        clusterElementDefinition.getName(), mcpTool.getParameters(),
                        ToolParameterDefaultsUtils.collectPropertyDefaults(clusterElementDefinition.getProperties()),
                        integrationId, mcpComponent.getMcpServerId(), environment, tenantId),
                    tenantId, authentication))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        String toolDescription = getToolDescription(mcpTool.getParameters(), null);

        if (toolDescription == null) {
            toolDescription = clusterElementDefinition.getDescription();
        }

        if (toolDescription != null) {
            builder.description(toolDescription);
        }

        ToolCallback toolCallback = builder.build();

        Long toolIntegrationId = integrationId == null
            ? fetchIntegrationId(mcpComponent.getComponentName())
            : integrationId;

        if (toolIntegrationId == null) {
            return toolCallback;
        }

        return EmbeddedMcpToolCallback.ofIntegration(toolCallback, toolIntegrationId);
    }

    public List<ToolCallback> getFunctionToolCallbacks(
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration, @Nullable String externalUserId,
        Environment environment, String tenantId, @Nullable Authentication authentication) {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfiguration.getId())) {
            if (!mcpIntegrationInstanceConfigurationWorkflow.isEnabled()) {
                continue;
            }

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

            Long integrationInstanceId = fetchIntegrationInstanceIdByConfiguration(
                externalUserId, integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId(),
                environment);

            if (integrationInstanceId != null &&
                !isWorkflowEnabled(integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflow)) {
                continue;
            }

            Workflow workflow = workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

            WorkflowTrigger trigger = getMcpToolCallableTrigger(workflow);

            if (trigger == null) {
                continue;
            }

            Map<String, ?> workflowParameters = mcpIntegrationInstanceConfigurationWorkflow.getParameters();

            ToolCallback toolCallback = createWorkflowToolCallback(
                workflow, workflowParameters,
                toolName -> withCallerContext(
                    getWorkflowToolCallbackFunction(
                        toolName, externalUserId, integration.getComponentName(), integration.getId(),
                        integrationInstanceConfigurationWorkflow, trigger.getName(), workflowParameters,
                        ToolParameterDefaultsUtils.collectTriggerInputDefaults(trigger.getParameters()),
                        mcpIntegrationInstanceConfiguration.getMcpServerId(), environment, tenantId),
                    tenantId, authentication));

            toolCallbacks.add(EmbeddedMcpToolCallback.ofIntegration(toolCallback, integration.getId()));
        }

        return toolCallbacks;
    }

    public List<ToolCallback> getAutomationToolCallbacks(
        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration, @Nullable String externalUserId,
        Environment environment, String tenantId, @Nullable Authentication authentication) {

        if (externalUserId == null) {
            return List.of();
        }

        Long connectedUserId = fetchConnectedUserId(externalUserId, environment);

        if (connectedUserId == null) {
            return List.of();
        }

        Map<Long, Set<Long>> enabledConnectedUserProjectWorkflowIds = getEnabledConnectedUserProjectWorkflowIds(
            connectedUserId);

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow : mcpAutomationProjectConfigurationWorkflowService
            .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())) {

            Set<Long> connectedUserProjectWorkflowIds = enabledConnectedUserProjectWorkflowIds.getOrDefault(
                mcpAutomationProjectConfigurationWorkflow.getId(), Set.of());

            if (!mcpAutomationProjectConfigurationWorkflow.isEnabled() || connectedUserProjectWorkflowIds.isEmpty()) {
                continue;
            }

            try {
                ToolCallback toolCallback = getAutomationToolCallback(
                    mcpAutomationProjectConfiguration, mcpAutomationProjectConfigurationWorkflow,
                    connectedUserProjectWorkflowIds, externalUserId, connectedUserId, environment, tenantId,
                    authentication);

                if (toolCallback != null) {
                    toolCallbacks.add(toolCallback);
                }
            } catch (RuntimeException exception) {
                log.warn(
                    "Skipping the automation tool of MCP automation project configuration workflow {}",
                    mcpAutomationProjectConfigurationWorkflow.getId(), exception);
            }
        }

        return toolCallbacks;
    }

    private @Nullable ToolCallback getAutomationToolCallback(
        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration,
        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow,
        Set<Long> connectedUserProjectWorkflowIds, String externalUserId, long connectedUserId,
        Environment environment, String tenantId, @Nullable Authentication authentication) {

        Optional<CallableAutomationDTO> callableAutomationOptional = connectedUserMcpAutomationResolver
            .fetchCallableAutomation(
                connectedUserId, mcpAutomationProjectConfiguration.getAutomationProjectConfigurationId(),
                mcpAutomationProjectConfigurationWorkflow.getWorkflowUuid());

        if (callableAutomationOptional.isEmpty()) {
            return null;
        }

        CallableAutomationDTO callableAutomation = callableAutomationOptional.get();

        if (!connectedUserProjectWorkflowIds.contains(callableAutomation.connectedUserProjectWorkflowId())) {
            return null;
        }

        String triggerName = callableAutomation.workflowCallTriggerName();

        if (triggerName == null) {
            return null;
        }

        Workflow workflow = workflowService.getWorkflow(callableAutomation.workflowId());

        Map<String, ?> workflowParameters = mcpAutomationProjectConfigurationWorkflow.getParameters();
        Map<String, Object> triggerInputDefaults = getTriggerInputDefaults(workflow, triggerName);

        return createWorkflowToolCallback(
            workflow, workflowParameters,
            toolName -> withCallerContext(
                getAutomationToolCallbackFunction(
                    toolName, externalUserId, connectedUserId, callableAutomation, triggerName, workflowParameters,
                    triggerInputDefaults, mcpAutomationProjectConfiguration.getMcpServerId(), environment),
                tenantId, authentication));
    }

    private Function<Map<String, Object>, Object> getAutomationToolCallbackFunction(
        String toolName, String externalUserId, long connectedUserId, CallableAutomationDTO callableAutomation,
        String triggerName, Map<String, ?> workflowParameters, Map<String, Object> triggerInputDefaults,
        long mcpServerId, Environment environment) {

        return inputParameters -> {
            checkMcpServerEnabled(mcpServerId);

            ExternalInvocationEvent.Builder eventBuilder = ExternalInvocationEvent
                .builder(PlatformType.EMBEDDED, ExternalInvocationSource.MCP_SERVER, ExternalInvocationKind.WORKFLOW,
                    toolName)
                .environment(environment.ordinal())
                .externalUserId(externalUserId)
                .connectedUserId(connectedUserId)
                .mcpServerId(mcpServerId);

            String missingConnectionComponentName = callableAutomation.missingConnectionComponentName();

            if (missingConnectionComponentName != null) {
                eventBuilder.componentName(missingConnectionComponentName);

                recordConnectionRequired(eventBuilder);

                return getAutomationConnectionRequiredResponse(missingConnectionComponentName);
            }

            JobParametersDTO jobParametersDTO = new JobParametersDTO(
                callableAutomation.workflowId(),
                getWorkflowJobInputs(
                    callableAutomation.inputs(), triggerName, workflowParameters, triggerInputDefaults,
                    inputParameters));

            return runWorkflowJob(
                eventBuilder,
                () -> callSkippingAutomationChecks(
                    () -> principalJobFacade.createJob(
                        jobParametersDTO, callableAutomation.projectDeploymentId(), PlatformType.AUTOMATION)));
        };
    }

    private ToolCallback createWorkflowToolCallback(
        Workflow workflow, Map<String, ?> workflowParameters,
        Function<String, Function<Map<String, Object>, Object>> toolCallbackFunctionFactory) {

        String toolName = Objects.requireNonNull(
            ToolNameUtils.getWorkflowToolName(workflowParameters, workflow.getLabel()),
            () -> "Workflow %s exposes no tool name: configure one or give the workflow a label"
                .formatted(workflow.getId()));

        List<FromAiResult> fromAiResults = extractFromAiResults(workflowParameters);

        FunctionToolCallback.Builder<Map<String, Object>, Object> builder = FunctionToolCallback
            .builder(toolName, toolCallbackFunctionFactory.apply(toolName))
            .inputType(Map.class)
            .inputSchema(FromAiInputSchemaUtils.generateInputSchema(fromAiResults));

        String description = getToolDescription(workflowParameters, null);

        if (description == null) {
            description = workflow.getDescription();
        }

        if (description != null) {
            builder.description(description);
        }

        return builder.build();
    }

    private Map<Long, Set<Long>> getEnabledConnectedUserProjectWorkflowIds(long connectedUserId) {
        Map<Long, Set<Long>> enabledConnectedUserProjectWorkflowIds = new HashMap<>();

        for (McpConnectedUserProjectWorkflow mcpConnectedUserProjectWorkflow : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            if (!mcpConnectedUserProjectWorkflow.isEnabled()) {
                continue;
            }

            Set<Long> connectedUserProjectWorkflowIds = enabledConnectedUserProjectWorkflowIds.computeIfAbsent(
                mcpConnectedUserProjectWorkflow.getMcpAutomationProjectConfigurationWorkflowId(),
                mcpAutomationProjectConfigurationWorkflowId -> new HashSet<>());

            connectedUserProjectWorkflowIds.add(mcpConnectedUserProjectWorkflow.getConnectedUserProjectWorkflowId());
        }

        return enabledConnectedUserProjectWorkflowIds;
    }

    private @Nullable Long fetchConnectedUserId(@Nullable String externalUserId, Environment environment) {
        if (externalUserId == null) {
            return null;
        }

        return connectedUserService.fetchConnectedUser(externalUserId, environment)
            .map(ConnectedUser::getId)
            .orElse(null);
    }

    private @Nullable IntegrationInstance fetchIntegrationInstance(
        @Nullable String externalUserId, String componentName, @Nullable Long integrationId,
        Environment environment) {

        return fetchConnectedUserIntegrationInstance(
            fetchConnectedUserId(externalUserId, environment), componentName, integrationId, environment);
    }

    private @Nullable IntegrationInstance fetchConnectedUserIntegrationInstance(
        @Nullable Long connectedUserId, String componentName, @Nullable Long integrationId,
        Environment environment) {

        if (connectedUserId == null) {
            return null;
        }

        Optional<IntegrationInstance> integrationInstance = integrationId == null
            ? integrationInstanceService.fetchIntegrationInstance(connectedUserId, componentName, environment)
            : integrationInstanceService.fetchIntegrationInstanceByIntegrationId(
                connectedUserId, integrationId, environment);

        return integrationInstance.orElse(null);
    }

    private @Nullable Long fetchMcpComponentIntegrationId(McpComponent mcpComponent) {
        return mcpIntegrationComponentService.fetchMcpIntegrationComponent(mcpComponent.getId())
            .map(McpIntegrationComponent::getIntegrationId)
            .orElse(null);
    }

    private @Nullable Long fetchIntegrationInstanceIdByConfiguration(
        @Nullable String externalUserId, long integrationInstanceConfigurationId, Environment environment) {

        return fetchConnectedUserIntegrationInstanceId(
            fetchConnectedUserId(externalUserId, environment), integrationInstanceConfigurationId);
    }

    private @Nullable Long fetchConnectedUserIntegrationInstanceId(
        @Nullable Long connectedUserId, long integrationInstanceConfigurationId) {

        if (connectedUserId == null) {
            return null;
        }

        return integrationInstanceService.fetchIntegrationInstance(connectedUserId, integrationInstanceConfigurationId)
            .map(IntegrationInstance::getId)
            .orElse(null);
    }

    private Function<Map<String, Object>, Object> getClusterElementToolCallbackFunction(
        String toolName, @Nullable String externalUserId, String componentName, int componentVersion,
        String clusterElementName, Map<String, ?> parameters, Map<String, Object> propertyDefaults,
        @Nullable Long integrationId, long mcpServerId, Environment environment, String tenantId) {

        return request -> {
            checkMcpServerEnabled(mcpServerId);

            Long connectedUserId = fetchConnectedUserId(externalUserId, environment);

            IntegrationInstance integrationInstance = fetchConnectedUserIntegrationInstance(
                connectedUserId, componentName, integrationId, environment);

            Long connectionId = integrationInstance == null ? null : integrationInstance.getConnectionId();

            ExternalInvocationEvent.Builder eventBuilder = ExternalInvocationEvent
                .builder(PlatformType.EMBEDDED, ExternalInvocationSource.MCP_SERVER,
                    ExternalInvocationKind.COMPONENT,
                    toolName)
                .componentName(componentName)
                .componentVersion(componentVersion)
                .operationName(clusterElementName)
                .connectionId(connectionId)
                .environment(environment.ordinal())
                .externalUserId(externalUserId)
                .connectedUserId(connectedUserId)
                .integrationInstanceId(integrationInstance == null ? null : integrationInstance.getId())
                .mcpServerId(mcpServerId);

            if (connectionId == null
                && isConnectionRequired(componentDefinitionService, componentName, componentVersion)) {

                if (externalUserId == null) {
                    throw new IllegalStateException(getConnectedUserRequiredMessage(componentName));
                }

                return getRecordedConnectionRequiredResponse(
                    eventBuilder, componentName, environment, externalUserId,
                    integrationId == null ? getIntegrationId(componentName) : integrationId, tenantId);
            }

            Map<String, Object> resolvedParameters = new HashMap<>();

            for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                resolvedParameters.put(entry.getKey(), resolveParameterValue(entry.getValue(), request));
            }

            Map<String, Object> mergedParameters = EmbeddedToolConstants.withConnectedUserContext(
                ToolParameterDefaultsUtils.mergeParameters(
                    ToolParameterDefaultsUtils.removeClearedDefaults(propertyDefaults, parameters),
                    MapUtils.concat(request, resolvedParameters)),
                externalUserId, environment);

            return externalInvocationRecorder.record(
                eventBuilder,
                () -> clusterElementDefinitionFacade.executeTool(
                    componentName, componentVersion, clusterElementName, mergedParameters, connectionId));
        };
    }

    private @Nullable Long fetchIntegrationId(String componentName) {
        return integrationService.getIntegrations()
            .stream()
            .filter(integration -> componentName.equals(integration.getComponentName()))
            .map(Integration::getId)
            .findFirst()
            .orElse(null);
    }

    private long getIntegrationId(String componentName) {
        Long integrationId = fetchIntegrationId(componentName);

        if (integrationId == null) {
            throw new IllegalStateException("No integration found for component: " + componentName);
        }

        return integrationId;
    }

    private Function<Map<String, Object>, Object> getWorkflowToolCallbackFunction(
        String toolName, @Nullable String externalUserId, String componentName, long integrationId,
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String triggerName,
        Map<String, ?> workflowParameters, Map<String, Object> triggerInputDefaults, long mcpServerId,
        Environment environment, String tenantId) {

        return inputParameters -> {
            checkMcpServerEnabled(mcpServerId);

            Long connectedUserId = fetchConnectedUserId(externalUserId, environment);

            Long integrationInstanceId = fetchConnectedUserIntegrationInstanceId(
                connectedUserId, integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

            ExternalInvocationEvent.Builder eventBuilder = ExternalInvocationEvent
                .builder(PlatformType.EMBEDDED, ExternalInvocationSource.MCP_SERVER, ExternalInvocationKind.WORKFLOW,
                    toolName)
                .componentName(componentName)
                .environment(environment.ordinal())
                .externalUserId(externalUserId)
                .connectedUserId(connectedUserId)
                .integrationInstanceId(integrationInstanceId)
                .mcpServerId(mcpServerId);

            if (integrationInstanceId == null) {
                if (externalUserId == null) {
                    throw new IllegalStateException(getConnectedUserRequiredMessage(componentName));
                }

                return getRecordedConnectionRequiredResponse(
                    eventBuilder, componentName, environment, externalUserId, integrationId, tenantId);
            }

            JobParametersDTO jobParametersDTO = new JobParametersDTO(
                integrationInstanceConfigurationWorkflow.getWorkflowId(),
                getWorkflowJobInputs(
                    integrationInstanceConfigurationWorkflow.getInputs(), triggerName, workflowParameters,
                    triggerInputDefaults, inputParameters));

            return runWorkflowJob(
                eventBuilder,
                () -> principalJobFacade.createJob(jobParametersDTO, integrationInstanceId, PlatformType.EMBEDDED));
        };
    }

    private Map<String, Object> getWorkflowJobInputs(
        Map<String, ?> workflowInputs, String triggerName, Map<String, ?> workflowParameters,
        Map<String, Object> triggerInputDefaults, Map<String, Object> inputParameters) {

        Map<String, Object> inputs = new HashMap<>(workflowInputs);

        Map<String, Object> triggerInputs = new HashMap<>();

        for (Map.Entry<String, ?> entry : workflowParameters.entrySet()) {
            triggerInputs.put(entry.getKey(), resolveParameterValue(entry.getValue(), inputParameters));
        }

        triggerInputs.putAll(inputParameters);

        if (!triggerInputDefaults.isEmpty()) {
            triggerInputs = ToolParameterDefaultsUtils.mergeParameters(
                ToolParameterDefaultsUtils.removeClearedDefaults(triggerInputDefaults, workflowParameters),
                triggerInputs);
        }

        inputs.put(triggerName, triggerInputs);
        inputs.put(JobInputConstants.TRIGGER_NAME_INPUT, triggerName);

        return inputs;
    }

    private @Nullable Object runWorkflowJob(ExternalInvocationEvent.Builder eventBuilder, LongSupplier jobCreator) {
        WorkflowToolRunResult workflowToolRunResult = externalInvocationRecorder.record(
            eventBuilder,
            () -> {
                long jobId = jobCreator.getAsLong();

                eventBuilder.jobId(jobId);

                WorkflowToolRunResult jobRunResult = getWorkflowRunResult(awaitJob(jobId));

                if (jobRunResult.status() == WorkflowToolRunResult.Status.STOPPED) {
                    eventBuilder.errorMessage(STOPPED_MESSAGE);
                }

                return jobRunResult;
            },
            WorkflowToolRunResult::getExternalInvocationStatus);

        return workflowToolRunResult.result();
    }

    private void checkMcpServerEnabled(long mcpServerId) {
        boolean enabled = mcpServerService.getEnabledMcpServers(PlatformType.EMBEDDED)
            .stream()
            .anyMatch(mcpServer -> Objects.equals(mcpServer.getId(), mcpServerId));

        if (!enabled) {
            throw new IllegalStateException("MCP server is disabled");
        }
    }

    private WorkflowToolRunResult getWorkflowRunResult(Job job) {
        if (job.getStatus() == Job.Status.STOPPED) {
            return describeStoppedJob(job);
        }

        JobExecutionErrors.checkForError(job, taskExecutionService);
        JobExecutionErrors.checkCompleted(job);

        if (job.getOutputs() == null) {
            return WorkflowToolRunResult.completed(null);
        }

        return WorkflowToolRunResult.completed(
            getCallableResponseOutput(job)
                .orElseGet(() -> taskFileStorage.readJobOutputs(job.getOutputs())));
    }

    private Job awaitJob(long jobId) {
        Duration timeout = JobCompletionAwaiter.DEFAULT_SYNC_TIMEOUT;

        CompletableFuture<Job> jobFuture = jobCompletionAwaiter.await(jobId, timeout);

        try {
            return jobFuture.join();
        } catch (CompletionException completionException) {
            if (completionException.getCause() instanceof TimeoutException timeoutException) {
                throw new IllegalStateException(
                    "Job %d did not finish within %d seconds".formatted(jobId, timeout.toSeconds()), timeoutException);
            }

            throw completionException;
        }
    }

    private WorkflowToolRunResult describeStoppedJob(Job job) {
        Map<String, Object> result = new HashMap<>();

        result.put("jobId", job.getId());

        WorkflowToolRunResult.Status status;

        if (isSuspendedForApproval(job)) {
            result.put("status", APPROVAL_REQUIRED_STATUS);
            result.put("message", "Approval required — the workflow run is paused waiting for a human decision.");

            status = WorkflowToolRunResult.Status.APPROVAL_REQUIRED;
        } else if (job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null) {
            result.put("status", "suspended");
            result.put("message", "The workflow run is paused and resumes later on its own.");

            status = WorkflowToolRunResult.Status.PAUSED;
        } else {
            result.put("status", "stopped");
            result.put("message", STOPPED_MESSAGE);

            status = WorkflowToolRunResult.Status.STOPPED;
        }

        return new WorkflowToolRunResult(status, result);
    }

    private boolean isSuspendedForApproval(Job job) {
        return findPausedTaskExecution(job)
            .map(TaskExecution::getType)
            .map(WorkflowNodeType::ofType)
            .map(WorkflowNodeType::name)
            .filter(APPROVAL_COMPONENT_NAMES::contains)
            .isPresent();
    }

    private Optional<TaskExecution> findPausedTaskExecution(Job job) {
        Object taskExecutionResumeId = job.getMetadata(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null && taskExecutionResumeId != null) {
            return Optional.of(
                taskExecutionService.getTaskExecution(ConvertUtils.convertValue(taskExecutionResumeId, Long.class)));
        }

        return taskExecutionService.fetchLastJobTaskExecution(Objects.requireNonNull(job.getId()))
            .filter(taskExecution -> taskExecution.getStatus() == TaskExecution.Status.STARTED);
    }

    private static Function<Map<String, Object>, Object> withCallerContext(
        Function<Map<String, Object>, Object> function, String tenantId, @Nullable Authentication authentication) {

        return request -> {
            String previousTenantId = TenantContext.getCurrentTenantId();

            TenantContext.setCurrentTenantId(tenantId);

            try {
                if (authentication == null) {
                    return runWithEmptySecurityContext(() -> function.apply(request));
                }

                return SecurityUtils.runAs(authentication, () -> function.apply(request));
            } finally {
                TenantContext.setCurrentTenantId(previousTenantId);
            }
        };
    }

    private static Object runWithEmptySecurityContext(Supplier<Object> supplier) {
        SecurityContext previousSecurityContext = SecurityContextHolder.getContext();

        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());

        try {
            return supplier.get();
        } finally {
            SecurityContextHolder.setContext(previousSecurityContext);
        }
    }

    private boolean isToolEnabled(long integrationInstanceId, long mcpToolId) {
        return mcpIntegrationInstanceToolService
            .fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId)
            .map(McpIntegrationInstanceTool::isEnabled)
            .orElse(false);
    }

    private boolean isWorkflowEnabled(
        long integrationInstanceId,
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {

        boolean mcpWorkflowEnabled = mcpIntegrationInstanceWorkflowService
            .fetchMcpIntegrationInstanceWorkflow(integrationInstanceId,
                mcpIntegrationInstanceConfigurationWorkflow.getId())
            .map(McpIntegrationInstanceWorkflow::isEnabled)
            .orElse(false);

        return mcpWorkflowEnabled && integrationInstanceWorkflowService
            .fetchIntegrationInstanceWorkflow(
                integrationInstanceId,
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId())
            .isPresent();
    }

    private Map<String, Object> getRecordedConnectionRequiredResponse(
        ExternalInvocationEvent.Builder eventBuilder, String componentName, Environment environment,
        String externalUserId, long integrationId, String tenantId) {

        Map<String, Object> connectionRequiredResponse = getConnectionRequiredResponse(
            componentName, environment, externalUserId, integrationId, tenantId);

        recordConnectionRequired(eventBuilder);

        return connectionRequiredResponse;
    }

    private void recordConnectionRequired(ExternalInvocationEvent.Builder eventBuilder) {
        externalInvocationRecorder.record(
            eventBuilder
                .status(ExternalInvocationStatus.CONNECTION_REQUIRED)
                .build());
    }

    private static Map<String, Object> getAutomationConnectionRequiredResponse(String componentName) {
        return Map.of(
            "error", "connection_required",
            "message",
            "The " + componentName + " connection is not set for this automation. " +
                "Instruct the user to connect " + componentName + " for this automation in the app, then try again.");
    }

    private Map<String, Object> getConnectionRequiredResponse(
        String componentName, Environment environment, String externalUserId, long integrationId, String tenantId) {
        String jwtToken = jwtTokenService.generateJwtToken(
            externalUserId, integrationId, environment.ordinal(), tenantId);

        String setupUrl = publicUrl + "/connect.html?token=" + jwtToken;

        return Map.of(
            "error", "connection_required",
            "message",
            "The " + componentName + " integration is not connected for this user. " +
                "To connect, visit: " + setupUrl + " . " +
                "Instruct the user to visit this link to connect their account. Present it as a markdown link " +
                "labelled \"Connect " + componentName + "\" instead of printing the raw URL.",
            "setupUrl", setupUrl);
    }

    private static String getConnectedUserRequiredMessage(String componentName) {
        return "The " + componentName + " tool requires a connected user, but this MCP server does not require " +
            "authentication, so the caller is anonymous. Enable authentication on the MCP server to use this tool.";
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
            log.warn(
                "Failed to extract callable response output from job {}: {}", job.getId(), exception.getMessage());

            return Optional.empty();
        }
    }

    static boolean isConnectionRequired(
        ComponentDefinitionService componentDefinitionService, String componentName, int componentVersion) {
        ComponentDefinition componentDefinition =
            componentDefinitionService.getComponentDefinition(componentName, componentVersion);

        return componentDefinition.isConnectionRequired();
    }

    private static <T> T callSkippingAutomationChecks(Supplier<T> supplier) {
        try {
            return AutomationAuthorizationContext.callSkippingChecks(supplier::get);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

    private static Map<String, Object> getTriggerInputDefaults(Workflow workflow, String triggerName) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            if (Objects.equals(workflowTrigger.getName(), triggerName)) {
                return ToolParameterDefaultsUtils.collectTriggerInputDefaults(workflowTrigger.getParameters());
            }
        }

        return Map.of();
    }

    private static @Nullable WorkflowTrigger getMcpToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.ofEnabled(workflow)) {
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
