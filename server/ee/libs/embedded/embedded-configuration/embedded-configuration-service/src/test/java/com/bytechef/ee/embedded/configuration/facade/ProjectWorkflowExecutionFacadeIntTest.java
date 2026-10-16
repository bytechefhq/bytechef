/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacade;
import com.bytechef.automation.workflow.execution.facade.ProjectWorkflowExecutionFacadeImpl;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.listener.AutomationWorkflowProjectPublishedEventListener;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserReferenceDeploymentService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.dto.WorkflowExecutionRowDTO;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.WorkflowExecutionRowService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        AutomationWorkflowProjectFacadeIntTestConfiguration.class,
        ConnectedUserReferenceRolloutManagerIntTest.ConnectedUserServiceConfiguration.class,
        ProjectWorkflowExecutionFacadeIntTest.ProjectWorkflowExecutionFacadeConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    ClusterElementDefinitionService.class, ComponentConnectionFacade.class, ComponentDefinitionService.class,
    ConnectionDefinitionService.class, ConnectionFacade.class, ConnectionLifecycleFacade.class,
    ConnectionService.class, ContextService.class, EmbeddedPermissionEvaluator.class, EnvironmentService.class,
    GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationInstanceConfigurationService.class, McpIntegrationInstanceConfigurationWorkflowService.class,
    McpIntegrationInstanceToolService.class, McpServerService.class, McpToolService.class,
    OAuth2ParametersFacade.class, OAuth2Service.class, PrincipalJobFacade.class, PrincipalJobService.class,
    ProjectFacade.class, TaskDispatcherDefinitionService.class, TaskExecutionService.class, TaskFileStorage.class,
    TriggerDefinitionFacade.class, TriggerDefinitionService.class, TriggerExecutionService.class,
    TriggerFileStorage.class, TriggerLifecycleFacade.class, UserService.class, WorkflowCacheManager.class,
    WorkflowNodeParameterFacade.class, WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class,
    WorkflowTestConfigurationService.class, WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
class ProjectWorkflowExecutionFacadeIntTest {

    private static final long COPY_JOB_ID = 1L;
    private static final long OTHER_USER_JOB_ID = 3L;
    private static final long REFERENCE_JOB_ID = 2L;

    @MockitoBean
    private AutomationWorkflowProjectPublishedEventListener automationWorkflowProjectPublishedEventListener;

    @Autowired
    private ConnectedUserProjectService connectedUserProjectService;

    @Autowired
    private ConnectedUserReferenceDeploymentService connectedUserReferenceDeploymentService;

    @Autowired
    private ConnectedUserService connectedUserService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectWorkflowExecutionFacade projectWorkflowExecutionFacade;

    @Autowired
    private ProjectWorkflowService projectWorkflowService;

    @MockitoBean
    private WorkflowExecutionRowService workflowExecutionRowService;

    private final List<Long> matchedJobIds = new ArrayList<>();
    private final List<ExecutionRow> executionRows = new ArrayList<>();

    @BeforeEach
    void beforeEach() {
        matchedJobIds.clear();
        executionRows.clear();

        when(workflowExecutionRowService.getWorkflowExecutionRows(
            any(), any(), any(), anyList(), any(), anyList(), anyBoolean(), anyList(), anyInt()))
                .thenAnswer(invocation -> {
                    List<Long> projectDeploymentIds = invocation.getArgument(3);
                    List<String> workflowIds = invocation.getArgument(5);

                    List<WorkflowExecutionRowDTO> rows = executionRows.stream()
                        .filter(executionRow -> projectDeploymentIds.contains(executionRow.projectDeploymentId()) &&
                            workflowIds.contains(executionRow.workflowId()))
                        .map(executionRow -> new WorkflowExecutionRowDTO(
                            WorkflowExecutionRowDTO.Kind.JOB, executionRow.jobId()))
                        .toList();

                    rows.forEach(row -> matchedJobIds.add(row.id()));

                    return new PageImpl<>(rows);
                });
    }

    @Test
    void testGetWorkflowExecutionsOfConnectedUserIncludesCopiesAndReferences() {
        long workspaceId = insertWorkspace();
        String suffix = UUID.randomUUID()
            .toString();

        ConnectedUser connectedUser = createConnectedUser("copy-and-reference-user-" + suffix);
        ConnectedUser otherConnectedUser = createConnectedUser("other-user-" + suffix);

        long copyProjectId = insertProject(workspaceId, "copy-" + suffix);
        long catalogProjectId = insertProject(workspaceId, "catalog-" + suffix);
        long otherUserProjectId = insertProject(workspaceId, "other-" + suffix);

        String copyWorkflowId = "copy-workflow-" + suffix;
        String referenceWorkflowId = "reference-workflow-" + suffix;
        String otherUserWorkflowId = "other-user-workflow-" + suffix;

        projectWorkflowService.addWorkflow(copyProjectId, 1, copyWorkflowId);
        projectWorkflowService.addWorkflow(catalogProjectId, 1, referenceWorkflowId);
        projectWorkflowService.addWorkflow(otherUserProjectId, 1, otherUserWorkflowId);

        connectedUserProjectService.create(Objects.requireNonNull(connectedUser.getId()), copyProjectId);
        connectedUserProjectService.create(Objects.requireNonNull(otherConnectedUser.getId()), otherUserProjectId);

        long copyProjectDeploymentId = insertProjectDeployment(copyProjectId, "__EMBEDDED__copy-" + suffix);
        long referenceProjectDeploymentId = insertProjectDeployment(
            catalogProjectId, "__EMBEDDED__reference-" + suffix);
        long otherUserProjectDeploymentId = insertProjectDeployment(
            otherUserProjectId, "__EMBEDDED__other-" + suffix);

        connectedUserReferenceDeploymentService.create(
            connectedUser.getId(), catalogProjectId, referenceProjectDeploymentId);

        executionRows.add(new ExecutionRow(COPY_JOB_ID, copyProjectDeploymentId, copyWorkflowId));
        executionRows.add(new ExecutionRow(REFERENCE_JOB_ID, referenceProjectDeploymentId, referenceWorkflowId));
        executionRows.add(new ExecutionRow(OTHER_USER_JOB_ID, otherUserProjectDeploymentId, otherUserWorkflowId));

        projectWorkflowExecutionFacade.getWorkflowExecutions(
            true, null, null, null, null, null, null, connectedUser.getId(), null, null, workspaceId, 0);

        assertThat(matchedJobIds).containsExactlyInAnyOrder(COPY_JOB_ID, REFERENCE_JOB_ID);
    }

    private ConnectedUser createConnectedUser(String externalUserId) {
        connectedUserService.createConnectedUser(externalUserId, Environment.PRODUCTION);

        return connectedUserService.getConnectedUser(externalUserId, Environment.PRODUCTION);
    }

    private long insertWorkspace() {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO workspace (name, created_date, created_by, last_modified_date, last_modified_by, version)
                    VALUES ('test', now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class),
            "workspace id");
    }

    private long insertProject(long workspaceId, String name) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project
                        (name, uuid, workspace_id, created_date, created_by, last_modified_date, last_modified_by,
                         version)
                    VALUES (?, gen_random_uuid(), ?, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, name, workspaceId),
            "project id");
    }

    private long insertProjectDeployment(long projectId, String name) {
        return Objects.requireNonNull(
            jdbcTemplate.queryForObject(
                """
                    INSERT INTO project_deployment
                        (project_id, project_version, name, enabled, environment, created_date, created_by,
                         last_modified_date, last_modified_by, version)
                    VALUES (?, 1, ?, true, 2, now(), 'test', now(), 'test', 0)
                    RETURNING id
                    """,
                Long.class, projectId, name),
            "project deployment id");
    }

    private record ExecutionRow(long jobId, long projectDeploymentId, String workflowId) {
    }

    @Configuration
    @Import(ProjectWorkflowExecutionFacadeImpl.class)
    static class ProjectWorkflowExecutionFacadeConfiguration {
    }
}
