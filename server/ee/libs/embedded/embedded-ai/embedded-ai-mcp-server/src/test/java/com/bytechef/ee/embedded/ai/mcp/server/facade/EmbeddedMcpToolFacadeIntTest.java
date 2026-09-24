/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.server.config.EmbeddedMcpServerIntTestConfiguration;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.encryption.Encryption;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor.JobFactoryFunction;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.mcp.repository.McpToolRepository;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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
class EmbeddedMcpToolFacadeIntTest {

    private static final String CLUSTER_ELEMENT_DESCRIPTION =
        "The POST method submits an entity to the specified resource.";
    private static final String CONNECTED_EXTERNAL_USER_ID = "external-user";
    private static final String NEW_WORKFLOW_CALL_TRIGGER_NAME = "newWorkflowCall_1";
    private static final String UNCONNECTED_EXTERNAL_USER_ID = "external-user-1";
    private static final String WORKFLOW_DESCRIPTION = "Sends an email to a customer";

    @MockitoBean
    private ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @MockitoBean
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @Autowired
    private EmbeddedMcpToolFacade embeddedMcpToolFacade;

    @Autowired
    private Encryption encryption;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JobCompletionAwaiter jobCompletionAwaiter;

    @MockitoBean
    private JobSyncExecutor jobSyncExecutor;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoSpyBean
    private McpComponentService mcpComponentService;

    @Autowired
    private McpComponentRepository mcpComponentRepository;

    @Autowired
    private McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;

    @MockitoSpyBean
    private McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;

    @Autowired
    private McpServerRepository mcpServerRepository;

    @Autowired
    private McpToolRepository mcpToolRepository;

    @MockitoBean
    private PrincipalJobFacade principalJobFacade;

    @MockitoBean
    private TaskExecutionService taskExecutionService;

    @MockitoBean
    private TaskFileStorage taskFileStorage;

    private long integrationInstanceId;
    private String workflowId;

    @AfterEach
    void afterEach() {
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM mcp_integration_instance_tool");
        jdbcTemplate.update("DELETE FROM integration_instance_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration_workflow");
        jdbcTemplate.update("DELETE FROM integration_instance_configuration");
        jdbcTemplate.update("DELETE FROM integration");
        jdbcTemplate.update("DELETE FROM connected_user");
        jdbcTemplate.update("DELETE FROM connection");
        jdbcTemplate.update("DELETE FROM workflow");

        mcpToolRepository.deleteAll();
        mcpComponentRepository.deleteAll();
        mcpServerRepository.deleteAll();
    }

    @Test
    void testClusterElementToolNameFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getClusterElementToolDefinition(Map.of());

        assertThat(toolDefinition.name()).isEqualTo("HTTPCLIENT_POST");
    }

    @Test
    void testClusterElementToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getClusterElementToolDefinition(Map.of("toolName", "postThing"));

        assertThat(toolDefinition.name()).isEqualTo("postThing");
    }

    @Test
    void testClusterElementToolDescriptionFallsBackToTheClusterElement() {
        ToolDefinition toolDefinition = getClusterElementToolDefinition(Map.of());

        assertThat(toolDefinition.description()).isEqualTo(CLUSTER_ELEMENT_DESCRIPTION);
    }

    @Test
    void testClusterElementToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getClusterElementToolDefinition(Map.of("toolDescription", "Posts a thing"));

        assertThat(toolDefinition.description()).isEqualTo("Posts a thing");
    }

    @Test
    void testClusterElementToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getClusterElementToolDefinition(Map.of("toolDescription", "   "));

        assertThat(toolDefinition.description()).isEqualTo(CLUSTER_ELEMENT_DESCRIPTION);
    }

    @Test
    void testGetFunctionToolCallbackReturnsNullWhenToolDisabled() {
        McpTool disabledMcpTool = saveMcpTool("disabled-tool", Map.of(), false);

        assertThat(
            embeddedMcpToolFacade.getFunctionToolCallback(
                disabledMcpTool, CONNECTED_EXTERNAL_USER_ID, Environment.PRODUCTION, "tenant"))
                    .isNull();

        verifyNoInteractions(mcpComponentService, clusterElementDefinitionService, mcpIntegrationInstanceToolService);
    }

    @Test
    void testWorkflowToolNameFallsBackToTheWorkflowLabel() {
        ToolDefinition toolDefinition = getUnconnectedWorkflowToolDefinition(Map.of());

        assertThat(toolDefinition.name()).isEqualTo("Send_Email");
    }

    @Test
    void testWorkflowToolNameUsesTheConfiguredName() {
        ToolDefinition toolDefinition = getUnconnectedWorkflowToolDefinition(Map.of("toolName", "sendEmail"));

        assertThat(toolDefinition.name()).isEqualTo("sendEmail");
    }

    @Test
    void testWorkflowToolDescriptionFallsBackToTheWorkflowDescription() {
        ToolDefinition toolDefinition = getUnconnectedWorkflowToolDefinition(Map.of());

        assertThat(toolDefinition.description()).isEqualTo(WORKFLOW_DESCRIPTION);
    }

    @Test
    void testWorkflowToolDescriptionUsesTheConfiguredDescription() {
        ToolDefinition toolDefinition = getUnconnectedWorkflowToolDefinition(
            Map.of("toolDescription", "Emails the customer"));

        assertThat(toolDefinition.description()).isEqualTo("Emails the customer");
    }

    @Test
    void testWorkflowToolDescriptionFallsBackWhenTheConfiguredDescriptionIsBlank() {
        ToolDefinition toolDefinition = getUnconnectedWorkflowToolDefinition(Map.of("toolDescription", "   "));

        assertThat(toolDefinition.description()).isEqualTo(WORKFLOW_DESCRIPTION);
    }

    @Test
    void testCallOfWorkflowToolSeedsReservedTriggerNameInput() {
        ToolCallback toolCallback = getConnectedWorkflowToolCallback(List.of());

        when(jobSyncExecutor.execute(any(), any(), anyBoolean(), any())).thenReturn(job(Job.Status.COMPLETED));

        toolCallback.call("{}");

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobSyncExecutor).execute(jobParametersDTOArgumentCaptor.capture(), any(), anyBoolean(), any());

        JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

        assertThat(jobParametersDTO.getInputs())
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, NEW_WORKFLOW_CALL_TRIGGER_NAME)
            .containsKey(NEW_WORKFLOW_CALL_TRIGGER_NAME);
    }

    @Test
    void testWorkflowToolRunsThroughTheSynchronousJobExecutor() {
        ToolCallback toolCallback = getConnectedWorkflowToolCallback(
            List.of(Map.of("name", "script_1", "type", "script/v1/java")));

        when(jobSyncExecutor.execute(any(), any(), anyBoolean(), any())).thenReturn(job(Job.Status.COMPLETED));

        toolCallback.call("{}");

        ArgumentCaptor<JobFactoryFunction> jobFactoryFunctionArgumentCaptor =
            ArgumentCaptor.forClass(JobFactoryFunction.class);

        verify(jobSyncExecutor).execute(any(), jobFactoryFunctionArgumentCaptor.capture(), anyBoolean(), any());

        JobParametersDTO jobParametersDTO = new JobParametersDTO(workflowId, Map.of());

        JobFactoryFunction jobFactoryFunction = jobFactoryFunctionArgumentCaptor.getValue();

        jobFactoryFunction.apply(jobParametersDTO);

        verify(principalJobFacade).createSyncJob(jobParametersDTO, integrationInstanceId, PlatformType.EMBEDDED);
        verify(principalJobFacade, never()).createJob(any(), anyLong(), any());
        verify(jobCompletionAwaiter, never()).await(anyLong(), any());
    }

    @Test
    void testWorkflowRunThatDidNotCompleteIsReportedAsAnError() {
        ToolCallback toolCallback = getConnectedWorkflowToolCallback(
            List.of(Map.of("name", "script_1", "type", "script/v1/java")));

        when(jobSyncExecutor.execute(any(), any(), anyBoolean(), any())).thenReturn(job(Job.Status.STARTED));

        assertThatThrownBy(() -> toolCallback.call("{}"))
            .hasMessageContaining("Workflow run 5 ended in status STARTED without completing.");
    }

    @Test
    void testWorkflowWithAnApprovalStepRunsAsADurableJobAndReportsApprovalRequired() {
        ToolCallback toolCallback = getConnectedWorkflowToolCallback(
            List.of(Map.of("name", "requestApproval_1", "type", "approval/v1/requestApproval")));

        Job job = job(Job.Status.STOPPED);

        job.setMetadata(
            Map.of(MetadataConstants.JOB_RESUME_ID, "resume-1", MetadataConstants.TASK_EXECUTION_RESUME_ID, 11L));

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(jobCompletionAwaiter.await(eq(5L), any())).thenReturn(CompletableFuture.completedFuture(job));
        when(taskExecutionService.getTaskExecution(11L))
            .thenReturn(
                taskExecution("requestApproval_1", "approval/v1/requestApproval", TaskExecution.Status.COMPLETED));

        Map<String, Object> result = readResult(toolCallback.call("{}"));

        assertThat(result)
            .containsEntry("status", "approval_required")
            .containsEntry("jobId", 5);

        verify(principalJobFacade).createJob(any(), eq(integrationInstanceId), eq(PlatformType.EMBEDDED));
        verify(jobSyncExecutor, never()).execute(any(), any(), anyBoolean(), any());
    }

    @Test
    void testManuallyStoppedJobIsReportedAsStoppedInsteadOfApprovalRequired() {
        ToolCallback toolCallback = getConnectedWorkflowToolCallback(
            List.of(Map.of("name", "requestApproval_1", "type", "approval/v1/requestApproval")));

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(jobCompletionAwaiter.await(eq(5L), any()))
            .thenReturn(CompletableFuture.completedFuture(job(Job.Status.STOPPED)));
        when(taskExecutionService.fetchLastJobTaskExecution(5L))
            .thenReturn(Optional.of(taskExecution("script_1", "script/v1/java", TaskExecution.Status.CANCELLED)));

        Map<String, Object> result = readResult(toolCallback.call("{}"));

        assertThat(result)
            .containsEntry("status", "stopped")
            .containsEntry("jobId", 5);
    }

    @Test
    void testTimedOutJobReturnsAnErrorThatNamesTheJob() {
        ToolCallback toolCallback = getConnectedWorkflowToolCallback(
            List.of(Map.of("name", "requestApproval_1", "type", "approval/v1/requestApproval")));

        when(principalJobFacade.createJob(any(), anyLong(), any())).thenReturn(5L);
        when(jobCompletionAwaiter.await(eq(5L), any()))
            .thenReturn(CompletableFuture.failedFuture(new TimeoutException("Job 5 did not finish within PT5M")));

        assertThatThrownBy(() -> toolCallback.call("{}"))
            .hasMessage("Job 5 did not finish within 300 seconds");
    }

    private ToolDefinition getClusterElementToolDefinition(Map<String, Object> parameters) {
        McpTool mcpTool = saveMcpTool("post", parameters, true);

        ClusterElementDefinition clusterElementDefinition = mock(ClusterElementDefinition.class);

        when(clusterElementDefinition.getComponentName()).thenReturn("httpClient");
        when(clusterElementDefinition.getComponentVersion()).thenReturn(1);
        when(clusterElementDefinition.getName()).thenReturn("post");
        when(clusterElementDefinition.getDescription()).thenReturn(CLUSTER_ELEMENT_DESCRIPTION);
        when(clusterElementDefinitionService.getClusterElementDefinition("httpClient", 1, "post"))
            .thenReturn(clusterElementDefinition);

        FunctionToolCallback<Map<String, Object>, Object> functionToolCallback =
            embeddedMcpToolFacade.getFunctionToolCallback(
                mcpTool, UNCONNECTED_EXTERNAL_USER_ID, Environment.PRODUCTION, "tenant");

        assertThat(functionToolCallback).isNotNull();

        return functionToolCallback.getToolDefinition();
    }

    private ToolCallback getConnectedWorkflowToolCallback(List<Map<String, String>> tasks) {
        List<ToolCallback> toolCallbacks = getWorkflowToolCallbacks(
            Map.of("toolName", "mappedTool", "toolDescription", "A mapped tool"), tasks, CONNECTED_EXTERNAL_USER_ID,
            true);

        assertThat(toolCallbacks).hasSize(1);

        return toolCallbacks.getFirst();
    }

    private ToolDefinition getUnconnectedWorkflowToolDefinition(Map<String, Object> toolParameters) {
        List<ToolCallback> toolCallbacks = getWorkflowToolCallbacks(
            toolParameters, List.of(), UNCONNECTED_EXTERNAL_USER_ID, false);

        assertThat(toolCallbacks).hasSize(1);

        ToolCallback toolCallback = toolCallbacks.getFirst();

        return toolCallback.getToolDefinition();
    }

    private List<ToolCallback> getWorkflowToolCallbacks(
        Map<String, Object> toolParameters, List<Map<String, String>> tasks, String externalUserId,
        boolean connected) {

        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.EMBEDDED, Environment.PRODUCTION, true));

        long environment = Environment.PRODUCTION.ordinal();

        long connectionId = insert(
            """
                INSERT INTO connection (name, component_name, environment, connection_version, parameters,
                    credential_status, type, created_date, created_by, last_modified_date, last_modified_by, version)
                VALUES ('myComponent', 'myComponent', ?, 1, '{}', 0, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            environment);

        long integrationId = insert(
            """
                INSERT INTO integration (name, component_name, allow_multiple_instances, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES ('myComponent', 'myComponent', false, now(), 'system', now(), 'system', 0)
                RETURNING id
                """);

        long integrationInstanceConfigurationId = insert(
            """
                INSERT INTO integration_instance_configuration (integration_id, integration_version, name, enabled,
                    environment, connection_parameters, authorization_type, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, 1, 'myComponent', true, ?, ?, 0, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationId, environment, encryption.encrypt("{}"));

        workflowId = String.valueOf(UUID.randomUUID());

        Map<String, Object> workflowDefinition = Map.of(
            "label", "Send Email",
            "description", WORKFLOW_DESCRIPTION,
            "triggers", List.of(
                Map.of(
                    "name", NEW_WORKFLOW_CALL_TRIGGER_NAME, "type", "workflow/v1/newWorkflowCall", "parameters",
                    Map.of())),
            "tasks", tasks);

        jdbcTemplate.update(
            """
                INSERT INTO workflow (id, definition, format, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, 0, now(), 'system', now(), 'system', 0)
                """,
            workflowId, JsonUtils.write(workflowDefinition));

        long integrationInstanceConfigurationWorkflowId = insert(
            """
                INSERT INTO integration_instance_configuration_workflow (integration_instance_configuration_id,
                    workflow_id, inputs, enabled, created_date, created_by, last_modified_date, last_modified_by,
                    version)
                VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            integrationInstanceConfigurationId, workflowId);

        long mcpIntegrationInstanceConfigurationId = insert(
            """
                INSERT INTO mcp_integration_instance_configuration (mcp_server_id,
                    integration_instance_configuration_id, created_date, created_by, last_modified_date,
                    last_modified_by, version)
                VALUES (?, ?, now(), 'system', now(), 'system', 0)
                RETURNING id
                """,
            mcpServer.getId(), integrationInstanceConfigurationId);

        jdbcTemplate.update(
            """
                INSERT INTO mcp_integration_instance_configuration_workflow (mcp_integration_instance_configuration_id,
                    integration_instance_configuration_workflow_id, parameters, created_date, created_by,
                    last_modified_date, last_modified_by, version)
                VALUES (?, ?, ?, now(), 'system', now(), 'system', 0)
                """,
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId,
            JsonUtils.write(toolParameters));

        if (connected) {
            long connectedUserId = insert(
                """
                    INSERT INTO connected_user (external_id, enabled, environment, created_date, created_by,
                        last_modified_date, last_modified_by, version)
                    VALUES (?, true, ?, now(), 'system', now(), 'system', 0)
                    RETURNING id
                    """,
                externalUserId, environment);

            integrationInstanceId = insert(
                """
                    INSERT INTO integration_instance (integration_instance_configuration_id, connected_user_id,
                        connection_id, enabled, created_date, created_by, last_modified_date, last_modified_by,
                        version)
                    VALUES (?, ?, ?, true, now(), 'system', now(), 'system', 0)
                    RETURNING id
                    """,
                integrationInstanceConfigurationId, connectedUserId, connectionId);

            jdbcTemplate.update(
                """
                    INSERT INTO integration_instance_workflow (integration_instance_id,
                        integration_instance_configuration_workflow_id, inputs, enabled, created_date, created_by,
                        last_modified_date, last_modified_by, version)
                    VALUES (?, ?, '{}', true, now(), 'system', now(), 'system', 0)
                    """,
                integrationInstanceId, integrationInstanceConfigurationWorkflowId);
        }

        List<McpIntegrationInstanceConfiguration> mcpIntegrationInstanceConfigurations =
            mcpIntegrationInstanceConfigurationService.getMcpServerMcpIntegrationInstanceConfigurations(
                Objects.requireNonNull(mcpServer.getId()));

        assertThat(mcpIntegrationInstanceConfigurations).hasSize(1);

        return embeddedMcpToolFacade.getFunctionToolCallbacks(
            mcpIntegrationInstanceConfigurations.getFirst(), externalUserId, Environment.PRODUCTION, "tenant-1");
    }

    private long insert(String sql, Object... arguments) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(sql, Long.class, arguments));
    }

    private McpTool saveMcpTool(String name, Map<String, Object> parameters, boolean enabled) {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer("test-server", PlatformType.EMBEDDED, Environment.PRODUCTION, true));

        McpComponent mcpComponent = mcpComponentRepository.save(
            new McpComponent("httpClient", 1, Objects.requireNonNull(mcpServer.getId()), null));

        McpTool mcpTool = new McpTool(name, parameters, Objects.requireNonNull(mcpComponent.getId()));

        mcpTool.setEnabled(enabled);

        return mcpToolRepository.save(mcpTool);
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

    @Configuration
    static class FacadeIntTestConfiguration {

        @Bean
        EmbeddedMcpToolFacade embeddedMcpToolFacade(
            ClusterElementDefinitionFacade clusterElementDefinitionFacade,
            ClusterElementDefinitionService clusterElementDefinitionService,
            ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
            Evaluator evaluator, IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
            IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
            IntegrationInstanceService integrationInstanceService,
            IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
            IntegrationService integrationService, JobCompletionAwaiter jobCompletionAwaiter,
            JobSyncExecutor jobSyncExecutor, JwtTokenService jwtTokenService, McpComponentService mcpComponentService,
            McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
            McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpServerService mcpServerService,
            PrincipalJobFacade principalJobFacade, TaskExecutionService taskExecutionService,
            TaskFileStorage taskFileStorage, WorkflowService workflowService) {

            return new EmbeddedMcpToolFacade(
                clusterElementDefinitionFacade, clusterElementDefinitionService, componentDefinitionService,
                connectedUserService, evaluator, integrationInstanceConfigurationService,
                integrationInstanceConfigurationWorkflowService, integrationInstanceService,
                integrationInstanceWorkflowService, integrationService, jobCompletionAwaiter, jobSyncExecutor,
                jwtTokenService, mcpComponentService, mcpIntegrationInstanceConfigurationWorkflowService,
                mcpIntegrationInstanceToolService, mcpServerService, principalJobFacade, "http://localhost:8080",
                taskExecutionService, taskFileStorage, workflowService);
        }
    }
}
