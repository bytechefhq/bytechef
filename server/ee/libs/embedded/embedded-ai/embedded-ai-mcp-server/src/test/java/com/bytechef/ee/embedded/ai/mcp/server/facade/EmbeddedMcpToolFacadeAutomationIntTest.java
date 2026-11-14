/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.server.config.EmbeddedMcpServerIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserMcpAutomationResolver;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationEvent;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationSource;
import com.bytechef.platform.external.invocation.ExternalInvocationStatus;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(
    classes = {
        EmbeddedMcpServerIntTestConfiguration.class, EmbeddedMcpToolFacadeIntTest.FacadeIntTestConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.workflow.repository.jdbc.enabled=true"
    })
@RecordApplicationEvents
class EmbeddedMcpToolFacadeAutomationIntTest {

    private static final String EXTERNAL_USER_ID = "automation-user";
    private static final long PROJECT_DEPLOYMENT_ID = 4242L;
    private static final String TENANT_ID = "tenant-1";
    private static final String TRIGGER_NAME = "newWorkflowCall_1";
    private static final String WORKFLOW_CALL_TRIGGER_TYPE = "workflow/v1/newWorkflowCall";
    private static final String WORKFLOW_DESCRIPTION = "Posts a message to a channel";
    private static final String WORKFLOW_LABEL = "Post Message";

    @Autowired
    private ApplicationEvents applicationEvents;

    @MockitoBean
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @MockitoBean
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @MockitoBean
    private ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver;

    @Autowired
    private EmbeddedMcpToolFacade embeddedMcpToolFacade;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JobCompletionAwaiter jobCompletionAwaiter;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @Autowired
    private McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;

    @Autowired
    private McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;

    @Autowired
    private McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @MockitoBean
    private PrincipalJobFacade principalJobFacade;

    @MockitoBean
    private TaskExecutionService taskExecutionService;

    @MockitoBean
    private TaskFileStorage taskFileStorage;

    private long automationProjectConfigurationId;
    private long connectedUserId;
    private long connectedUserProjectId;
    private McpAutomationProjectConfiguration mcpAutomationProjectConfiguration;
    private long mcpServerId;
    private final Map<String, String> workflowCallTriggerNameByWorkflowId = new HashMap<>();

    @BeforeEach
    void beforeEach() {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("automation-server", PlatformType.EMBEDDED, Environment.PRODUCTION, true));

        mcpServerId = Objects.requireNonNull(mcpServer.getId());

        long workspaceId = insert(
            """
                INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('test', now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        long projectId = insert(
            """
                INSERT INTO project (name, uuid, workspace_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES ('template', gen_random_uuid(), ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            workspaceId);

        automationProjectConfigurationId = insert(
            """
                INSERT INTO automation_project_configuration (project_id, environment_id, project_version, enabled,
                    automation_hub_visible, name, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, 1, true, true, 'configuration', now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            projectId, Environment.PRODUCTION.ordinal());

        connectedUserId = insert(
            """
                INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, true, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            EXTERNAL_USER_ID, Environment.PRODUCTION.ordinal());

        connectedUserProjectId = insert(
            """
                INSERT INTO connected_user_project (connected_user_id, project_id, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            connectedUserId, projectId);

        mcpAutomationProjectConfiguration = mcpAutomationProjectConfigurationService.create(
            new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServerId));
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_connected_user_project_workflow");
        jdbcTemplate.update("DELETE FROM mcp_automation_project_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_automation_project_configuration");
        jdbcTemplate.update("DELETE FROM connected_user_project_workflow");
        jdbcTemplate.update("DELETE FROM connected_user_project");
        jdbcTemplate.update("DELETE FROM automation_project_configuration");
        jdbcTemplate.update("DELETE FROM project_deployment_workflow");
        jdbcTemplate.update("DELETE FROM project_deployment");
        jdbcTemplate.update("DELETE FROM project");
        jdbcTemplate.update("DELETE FROM workspace");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM workflow");

        mcpServerRepository.deleteAll();
    }

    @Test
    void testAutomationToolListedForActivatedUser() {
        addAutomationTool(
            Map.of("toolName", "postToChannel", "toolDescription", "Posts to the channel"), callableWorkflowId(),
            true, null);
        addAutomationTool(Map.of(), callableWorkflowId(), true, null);

        List<ToolCallback> toolCallbacks = getAutomationToolCallbacks(EXTERNAL_USER_ID);

        assertThat(toolCallbacks)
            .extracting(ToolCallback::getToolDefinition)
            .extracting(ToolDefinition::name, ToolDefinition::description)
            .containsExactlyInAnyOrder(
                tuple("postToChannel", "Posts to the channel"), tuple("Post_Message", WORKFLOW_DESCRIPTION));
    }

    @Test
    void testAutomationToolHiddenForAnonymousCaller() {
        addAutomationTool(Map.of(), callableWorkflowId(), true, null);

        assertThat(getAutomationToolCallbacks(null)).isEmpty();
        assertThat(getAutomationToolCallbacks("unknown-user")).isEmpty();

        verifyNoInteractions(connectedUserMcpAutomationResolver);
    }

    @Test
    void testAutomationToolHiddenWhenRowDisabled() {
        addAutomationTool(Map.of(), callableWorkflowId(), false, null);

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolHiddenWithoutRow() {
        String workflowUuid = String.valueOf(UUID.randomUUID());

        long connectedUserProjectWorkflowId = insertReference(workflowUuid);

        createLinkWorkflow(workflowUuid, Map.of());
        stubCallableAutomation(workflowUuid, connectedUserProjectWorkflowId, callableWorkflowId(), null);

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolHiddenWhenRowBelongsToAnotherReference() {
        String workflowUuid = String.valueOf(UUID.randomUUID());

        long connectedUserProjectWorkflowId = insertReference(workflowUuid);
        long otherConnectedUserProjectWorkflowId = insertReference(String.valueOf(UUID.randomUUID()));

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow = createLinkWorkflow(
            workflowUuid, Map.of());

        mcpConnectedUserProjectWorkflowService.create(
            otherConnectedUserProjectWorkflowId,
            Objects.requireNonNull(mcpAutomationProjectConfigurationWorkflow.getId()),
            true);

        stubCallableAutomation(workflowUuid, connectedUserProjectWorkflowId, callableWorkflowId(), null);

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolHiddenWhenLinkWorkflowDisabled() {
        AutomationTool automationTool = addAutomationTool(Map.of(), callableWorkflowId(), true, null);

        mcpAutomationProjectConfigurationWorkflowService.updateEnabled(
            automationTool.mcpAutomationProjectConfigurationWorkflowId(), false);

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolHiddenWhenConfigurationDisabled() {
        AutomationTool automationTool = addAutomationTool(Map.of(), callableWorkflowId(), true, null);

        when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, automationProjectConfigurationId, automationTool.workflowUuid()))
                .thenReturn(Optional.empty());

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolStaysWhenAutomationSwitchedOff() {
        String workflowId = callableWorkflowId();

        addAutomationTool(Map.of(), workflowId, true, null);

        long projectDeploymentWorkflowId = insertProjectDeploymentWorkflow(workflowId);

        assertThat(
            jdbcTemplate.update(
                "UPDATE project_deployment_workflow SET enabled = false WHERE id = ?", projectDeploymentWorkflowId))
                    .isEqualTo(1);

        jdbcTemplate.update("UPDATE connected_user_project_workflow SET enabled = false");

        List<ToolCallback> toolCallbacks = getAutomationToolCallbacks(EXTERNAL_USER_ID);

        assertThat(toolCallbacks).hasSize(1);

        mockCompletedJob();

        ToolCallback toolCallback = toolCallbacks.getFirst();

        toolCallback.call("{}");

        verify(principalJobFacade).createJob(any(), eq(PROJECT_DEPLOYMENT_ID), eq(PlatformType.AUTOMATION));
    }

    @Test
    void testAutomationToolAnswersConnectionRequired() {
        insert(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('Slack', 'slack', false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        assertConnectionRequiredAnswer();
    }

    @Test
    void testAutomationToolAnswersConnectionRequiredWithoutIntegration() {
        assertConnectionRequiredAnswer();
    }

    @Test
    void testAutomationToolHiddenWithoutWorkflowCallTrigger() {
        addAutomationTool(
            Map.of(), insertWorkflow(WORKFLOW_LABEL, WORKFLOW_DESCRIPTION, "webhook_1", "webhook/v1/newWebhook"),
            true, null);

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolHiddenWhenResolverReportsNotToolCallable() {
        String workflowId = callableWorkflowId();

        AutomationTool automationTool = addAutomationTool(Map.of(), workflowId, true, null);

        CallableAutomationDTO callableAutomation = connectedUserMcpAutomationResolver
            .fetchCallableAutomation(connectedUserId, automationProjectConfigurationId, automationTool.workflowUuid())
            .orElseThrow();

        when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, automationProjectConfigurationId, automationTool.workflowUuid()))
                .thenReturn(
                    Optional.of(
                        new CallableAutomationDTO(
                            callableAutomation.connectedUserProjectWorkflowId(),
                            callableAutomation.projectDeploymentId(), workflowId, callableAutomation.inputs(), null,
                            null)));

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID)).isEmpty();
    }

    @Test
    void testAutomationToolSchemaFromDeploymentVersion() {
        String deploymentWorkflowId = insertWorkflow(
            "Old Post", "The deployed version", "oldWorkflowCall_1", WORKFLOW_CALL_TRIGGER_TYPE);

        insertWorkflow("New Post", "The configuration version", TRIGGER_NAME, WORKFLOW_CALL_TRIGGER_TYPE);

        addAutomationTool(
            Map.of("city", Map.of("name", "city", "required", true, "type", "STRING")), deploymentWorkflowId, true,
            null);

        List<ToolCallback> toolCallbacks = getAutomationToolCallbacks(EXTERNAL_USER_ID);

        assertThat(toolCallbacks).hasSize(1);

        ToolCallback toolCallback = toolCallbacks.getFirst();

        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        assertThat(toolDefinition.name()).isEqualTo("Old_Post");
        assertThat(toolDefinition.description()).isEqualTo("The deployed version");
        assertThat(toolDefinition.inputSchema()).contains("\"city\"");

        mockCompletedJob();

        toolCallback.call("{\"city\": \"Zagreb\"}");

        JobParametersDTO jobParametersDTO = captureJobParameters();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo(deploymentWorkflowId);
        assertThat(jobParametersDTO.getInputs())
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, "oldWorkflowCall_1")
            .containsEntry("oldWorkflowCall_1", Map.of("city", "Zagreb"));
    }

    @Test
    void testAutomationToolRunsCallersDeployment() {
        String workflowId = callableWorkflowId();

        addAutomationTool(Map.of("greeting", "Hello"), workflowId, true, null);

        boolean[] createdSkippingChecks = {
            false
        };

        when(principalJobFacade.createJob(any(), anyLong(), any()))
            .thenAnswer(invocation -> {
                createdSkippingChecks[0] = AutomationAuthorizationContext.isSkipChecks();

                return 5L;
            });
        when(jobCompletionAwaiter.await(eq(5L), any()))
            .thenReturn(CompletableFuture.completedFuture(job(Job.Status.COMPLETED)));

        ToolCallback toolCallback = getSingleAutomationToolCallback();

        toolCallback.call("{\"text\": \"hi\"}");

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade).createJob(
            jobParametersDTOArgumentCaptor.capture(), eq(PROJECT_DEPLOYMENT_ID), eq(PlatformType.AUTOMATION));
        verify(principalJobFacade, never()).createJob(any(), anyLong(), eq(PlatformType.EMBEDDED));

        JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo(workflowId);
        assertThat(jobParametersDTO.getInputs())
            .containsEntry("channel", "#general")
            .containsEntry(TRIGGER_NAME, Map.of("greeting", "Hello", "text", "hi"))
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, TRIGGER_NAME);
        assertThat(createdSkippingChecks[0]).isTrue();
        assertThat(AutomationAuthorizationContext.isSkipChecks()).isFalse();
    }

    @Test
    void testAutomationToolReturnsApprovalAndSuspendedResults() {
        addAutomationTool(Map.of(), callableWorkflowId(), true, null);

        ToolCallback toolCallback = getSingleAutomationToolCallback();

        Job approvalJob = job(Job.Status.STOPPED);

        approvalJob.setMetadata(
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.TASK_EXECUTION_RESUME_ID, 11L));

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(jobCompletionAwaiter.await(eq(5L), any())).thenReturn(CompletableFuture.completedFuture(approvalJob));
        when(taskExecutionService.getTaskExecution(11L))
            .thenReturn(
                taskExecution("requestApproval_1", "approval/v1/requestApproval", TaskExecution.Status.COMPLETED));

        assertThat(readResult(toolCallback.call("{}")))
            .containsEntry("status", "approval_required")
            .containsEntry("jobId", 5);

        Job suspendedJob = job(Job.Status.STOPPED);

        suspendedJob.setMetadata(
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-2", MetadataConstants.TASK_EXECUTION_RESUME_ID, 12L));

        when(jobCompletionAwaiter.await(eq(5L), any())).thenReturn(CompletableFuture.completedFuture(suspendedJob));
        when(taskExecutionService.getTaskExecution(12L))
            .thenReturn(taskExecution("delay_1", "delay/v1/delay", TaskExecution.Status.STARTED));

        assertThat(readResult(toolCallback.call("{}")))
            .containsEntry("status", "suspended")
            .containsEntry("jobId", 5);

        assertThat(getExternalInvocationEvents())
            .extracting(ExternalInvocationEvent::status)
            .containsExactly(ExternalInvocationStatus.APPROVAL_REQUIRED, ExternalInvocationStatus.SUSPENDED);
    }

    @Test
    void testAutomationToolRecordsExternalInvocationEvent() {
        addAutomationTool(Map.of("toolName", "postToChannel"), callableWorkflowId(), true, null);

        mockCompletedJob();

        ToolCallback toolCallback = getSingleAutomationToolCallback();

        toolCallback.call("{}");

        assertThat(getExternalInvocationEvents()).singleElement()
            .satisfies(externalInvocationEvent -> {
                assertThat(externalInvocationEvent.type()).isEqualTo(PlatformType.EMBEDDED);
                assertThat(externalInvocationEvent.source()).isEqualTo(ExternalInvocationSource.MCP_SERVER);
                assertThat(externalInvocationEvent.kind()).isEqualTo(ExternalInvocationKind.WORKFLOW);
                assertThat(externalInvocationEvent.invocationName()).isEqualTo("postToChannel");
                assertThat(externalInvocationEvent.environment()).isEqualTo(Environment.PRODUCTION.ordinal());
                assertThat(externalInvocationEvent.externalUserId()).isEqualTo(EXTERNAL_USER_ID);
                assertThat(externalInvocationEvent.connectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationEvent.mcpServerId()).isEqualTo(mcpServerId);
                assertThat(externalInvocationEvent.jobId()).isEqualTo(5L);
                assertThat(externalInvocationEvent.status()).isEqualTo(ExternalInvocationStatus.SUCCESS);
            });
    }

    @Test
    void testAutomationToolSkippedWhenResolverThrows() {
        AutomationTool failingAutomationTool = addAutomationTool(
            Map.of("toolName", "failingTool"), callableWorkflowId(), true, null);

        addAutomationTool(Map.of("toolName", "workingTool"), callableWorkflowId(), true, null);

        when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, automationProjectConfigurationId, failingAutomationTool.workflowUuid()))
                .thenThrow(new IllegalArgumentException("Invalid UUID string"));

        assertThat(getAutomationToolCallbacks(EXTERNAL_USER_ID))
            .extracting(ToolCallback::getToolDefinition)
            .extracting(ToolDefinition::name)
            .containsExactly("workingTool");
    }

    private AutomationTool addAutomationTool(
        Map<String, Object> parameters, String workflowId, boolean rowEnabled,
        @Nullable String missingConnectionComponentName) {

        String workflowUuid = String.valueOf(UUID.randomUUID());

        long connectedUserProjectWorkflowId = insertReference(workflowUuid);

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow = createLinkWorkflow(
            workflowUuid, parameters);

        long mcpAutomationProjectConfigurationWorkflowId = Objects.requireNonNull(
            mcpAutomationProjectConfigurationWorkflow.getId());

        mcpConnectedUserProjectWorkflowService.create(
            connectedUserProjectWorkflowId, mcpAutomationProjectConfigurationWorkflowId, rowEnabled);

        stubCallableAutomation(
            workflowUuid, connectedUserProjectWorkflowId, workflowId, missingConnectionComponentName);

        return new AutomationTool(workflowUuid, mcpAutomationProjectConfigurationWorkflowId);
    }

    private void assertConnectionRequiredAnswer() {
        addAutomationTool(Map.of("toolName", "postToChannel"), callableWorkflowId(), true, "slack");

        ToolCallback toolCallback = getSingleAutomationToolCallback();

        Map<String, Object> result = readResult(toolCallback.call("{}"));

        assertThat(result)
            .containsEntry("error", "connection_required")
            .doesNotContainKey("setupUrl");
        assertThat((String) result.get("message"))
            .contains("The slack connection is not set for this automation");
        assertThat(getExternalInvocationEvents()).singleElement()
            .satisfies(externalInvocationEvent -> {
                assertThat(externalInvocationEvent.status()).isEqualTo(ExternalInvocationStatus.CONNECTION_REQUIRED);
                assertThat(externalInvocationEvent.invocationName()).isEqualTo("postToChannel");
                assertThat(externalInvocationEvent.componentName()).isEqualTo("slack");
                assertThat(externalInvocationEvent.connectedUserId()).isEqualTo(connectedUserId);
                assertThat(externalInvocationEvent.jobId()).isNull();
            });

        verifyNoInteractions(jwtTokenService, principalJobFacade, jobCompletionAwaiter);
    }

    private String callableWorkflowId() {
        return insertWorkflow(WORKFLOW_LABEL, WORKFLOW_DESCRIPTION, TRIGGER_NAME, WORKFLOW_CALL_TRIGGER_TYPE);
    }

    private JobParametersDTO captureJobParameters() {
        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(principalJobFacade).createJob(jobParametersDTOArgumentCaptor.capture(), anyLong(), any());

        return jobParametersDTOArgumentCaptor.getValue();
    }

    private McpAutomationProjectConfigurationWorkflow createLinkWorkflow(
        String workflowUuid, Map<String, Object> parameters) {

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(
                Objects.requireNonNull(mcpAutomationProjectConfiguration.getId()), workflowUuid);

        return mcpAutomationProjectConfigurationWorkflowService.updateParameters(
            Objects.requireNonNull(mcpAutomationProjectConfigurationWorkflow.getId()), parameters);
    }

    private List<ToolCallback> getAutomationToolCallbacks(@Nullable String externalUserId) {
        return embeddedMcpToolFacade.getAutomationToolCallbacks(
            mcpAutomationProjectConfiguration, externalUserId, Environment.PRODUCTION, TENANT_ID);
    }

    private List<ExternalInvocationEvent> getExternalInvocationEvents() {
        return applicationEvents.stream(ExternalInvocationEvent.class)
            .toList();
    }

    private ToolCallback getSingleAutomationToolCallback() {
        List<ToolCallback> toolCallbacks = getAutomationToolCallbacks(EXTERNAL_USER_ID);

        assertThat(toolCallbacks).hasSize(1);

        return toolCallbacks.getFirst();
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }

    private long insertProjectDeploymentWorkflow(String workflowId) {
        long projectId = Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                "SELECT project_id FROM automation_project_configuration WHERE id = ?", Long.class,
                automationProjectConfigurationId));

        long projectDeploymentId = insert(
            """
                INSERT INTO project_deployment (project_id, project_version, name, enabled, environment,
                    created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, 1, 'deployment', true, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            projectId, Environment.PRODUCTION.ordinal());

        return insert(
            """
                INSERT INTO project_deployment_workflow (project_deployment_id, workflow_id, inputs, enabled,
                    created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            projectDeploymentId, workflowId);
    }

    private long insertReference(String workflowUuid) {
        return insert(
            """
                INSERT INTO connected_user_project_workflow (connected_user_project_id, automation_uuid, enabled,
                    dangling, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES (?, ?, true, false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            connectedUserProjectId, workflowUuid);
    }

    private String insertWorkflow(String label, String description, String triggerName, String triggerType) {
        String workflowId = String.valueOf(UUID.randomUUID());

        Map<String, Object> workflowDefinition = Map.of(
            "label", label,
            "description", description,
            "triggers", List.of(Map.of("name", triggerName, "type", triggerType, "parameters", Map.of())),
            "tasks", List.of());

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            workflowId, JsonUtils.write(workflowDefinition));

        if (WORKFLOW_CALL_TRIGGER_TYPE.equals(triggerType)) {
            workflowCallTriggerNameByWorkflowId.put(workflowId, triggerName);
        }

        return workflowId;
    }

    private void mockCompletedJob() {
        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(jobCompletionAwaiter.await(eq(5L), any()))
            .thenReturn(CompletableFuture.completedFuture(job(Job.Status.COMPLETED)));
    }

    private void stubCallableAutomation(
        String workflowUuid, long connectedUserProjectWorkflowId, String workflowId,
        @Nullable String missingConnectionComponentName) {

        when(connectedUserMcpAutomationResolver.fetchCallableAutomation(
            connectedUserId, automationProjectConfigurationId, workflowUuid))
                .thenReturn(
                    Optional.of(
                        new CallableAutomationDTO(
                            connectedUserProjectWorkflowId, PROJECT_DEPLOYMENT_ID, workflowId,
                            Map.of("channel", "#general"), missingConnectionComponentName,
                            workflowCallTriggerNameByWorkflowId.get(workflowId))));
    }

    private static Job job(Job.Status status) {
        Job job = new Job();

        job.setId(5L);
        job.setStatus(status);

        return job;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readResult(String result) {
        return JsonUtils.read(result, Map.class);
    }

    private static TaskExecution taskExecution(String name, String type, TaskExecution.Status status) {
        return TaskExecution.builder()
            .workflowTask(new WorkflowTask(Map.of("name", name, "type", type)))
            .status(status)
            .build();
    }

    private record AutomationTool(String workflowUuid, long mcpAutomationProjectConfigurationWorkflowId) {
    }
}
