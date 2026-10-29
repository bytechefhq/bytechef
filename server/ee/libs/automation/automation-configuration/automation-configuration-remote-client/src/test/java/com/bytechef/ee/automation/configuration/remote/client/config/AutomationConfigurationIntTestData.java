/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.config;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.repository.jdbc.JdbcContextRepository;
import com.bytechef.atlas.execution.repository.jdbc.JdbcJobRepository;
import com.bytechef.automation.ai.mcp.domain.WorkspaceMcpServer;
import com.bytechef.automation.ai.mcp.repository.WorkspaceMcpServerRepository;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.repository.ProjectDeploymentRepository;
import com.bytechef.automation.configuration.repository.ProjectDeploymentWorkflowRepository;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.ProjectWorkflowRepository;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.repository.TagRepository;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.workflow.execution.repository.PrincipalJobRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@SuppressFBWarnings({
    "EI2", "HARD_CODE_PASSWORD"
})
public class AutomationConfigurationIntTestData {

    private final ConnectionRepository connectionRepository;
    private final ConnectionService connectionService;
    private final JdbcContextRepository jdbcContextRepository;
    private final JdbcJobRepository jdbcJobRepository;
    private final McpServerRepository mcpServerRepository;
    private final PrincipalJobRepository principalJobRepository;
    private final ProjectDeploymentRepository projectDeploymentRepository;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectRepository projectRepository;
    private final ProjectService projectService;
    private final ProjectWorkflowRepository projectWorkflowRepository;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final WorkspaceMcpServerRepository workspaceMcpServerRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceUserRepository workspaceUserRepository;

    AutomationConfigurationIntTestData(
        ConnectionRepository connectionRepository, ConnectionService connectionService,
        JdbcContextRepository jdbcContextRepository, JdbcJobRepository jdbcJobRepository,
        McpServerRepository mcpServerRepository, PrincipalJobRepository principalJobRepository,
        ProjectDeploymentRepository projectDeploymentRepository,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowRepository projectDeploymentWorkflowRepository,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectRepository projectRepository,
        ProjectService projectService, ProjectWorkflowRepository projectWorkflowRepository,
        ProjectWorkflowService projectWorkflowService, TagRepository tagRepository, UserRepository userRepository,
        WorkflowService workflowService, WorkspaceMcpServerRepository workspaceMcpServerRepository,
        WorkspaceRepository workspaceRepository, WorkspaceUserRepository workspaceUserRepository) {

        this.connectionRepository = connectionRepository;
        this.connectionService = connectionService;
        this.jdbcContextRepository = jdbcContextRepository;
        this.jdbcJobRepository = jdbcJobRepository;
        this.mcpServerRepository = mcpServerRepository;
        this.principalJobRepository = principalJobRepository;
        this.projectDeploymentRepository = projectDeploymentRepository;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowRepository = projectDeploymentWorkflowRepository;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectRepository = projectRepository;
        this.projectService = projectService;
        this.projectWorkflowRepository = projectWorkflowRepository;
        this.projectWorkflowService = projectWorkflowService;
        this.tagRepository = tagRepository;
        this.userRepository = userRepository;
        this.workflowService = workflowService;
        this.workspaceMcpServerRepository = workspaceMcpServerRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    public void deleteAll() {
        principalJobRepository.deleteAll();
        jdbcContextRepository.deleteAll();
        jdbcJobRepository.deleteAll();
        projectDeploymentWorkflowRepository.deleteAll();
        projectDeploymentRepository.deleteAll();
        projectWorkflowRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceUserRepository.deleteAll();
        workspaceMcpServerRepository.deleteAll();
        mcpServerRepository.deleteAll();
        workspaceRepository.deleteAll();
        connectionRepository.deleteAll();
        tagRepository.deleteAll();
        userRepository.deleteAll();

        List<String> workflowIds = workflowService.getWorkflows()
            .stream()
            .map(Workflow::getId)
            .toList();

        workflowService.delete(workflowIds);
    }

    public Connection createConnection(String name) {
        return connectionService.create(
            Connection.builder()
                .authorizationType(AuthorizationType.BEARER_TOKEN)
                .componentName("airtable")
                .connectionVersion(1)
                .name(name)
                .parameters(Map.of("token", "secret"))
                .type(PlatformType.AUTOMATION)
                .build());
    }

    public McpServer createMcpServer(String name, long workspaceId) {
        McpServer mcpServer = mcpServerRepository.save(
            new McpServer(name, PlatformType.AUTOMATION, Environment.PRODUCTION));

        workspaceMcpServerRepository.save(new WorkspaceMcpServer(mcpServer.getId(), workspaceId));

        return mcpServer;
    }

    public Project createProject(String name, long workspaceId) {
        Project project = new Project();

        project.setName(name);
        project.setProjectVersions(List.of(new ProjectVersion(1)));
        project.setWorkspaceId(workspaceId);

        return projectService.create(project);
    }

    public ProjectDeployment createProjectDeployment(
        String name, long projectId, int projectVersion, Environment environment, List<Long> tagIds) {

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setTagIds(tagIds);

        return projectDeploymentService.create(projectDeployment);
    }

    public ProjectDeploymentWorkflow createProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowId, boolean enabled, Map<String, ?> inputs,
        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(projectDeploymentWorkflowConnections);
        projectDeploymentWorkflow.setEnabled(enabled);
        projectDeploymentWorkflow.setInputs(inputs);
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflowService.create(projectDeploymentWorkflow);
    }

    public ProjectWorkflow createProjectWorkflow(long projectId, int projectVersion, String label, UUID uuid) {
        Workflow workflow = createWorkflow(label);

        return projectWorkflowService.addWorkflow(projectId, projectVersion, workflow.getId(), uuid);
    }

    public Tag createTag(String name) {
        return tagRepository.save(new Tag(name));
    }

    public User createUser(String login) {
        User user = new User();

        user.setActivated(true);
        user.setEmail(login + "@example.com");
        user.setLogin(login);
        user.setPassword("password-hash");

        return userRepository.save(user);
    }

    public Workflow createWorkflow(String label) {
        String definition = """
            {"label":"LABEL","triggers":[{"name":"trigger_1","type":"workflow/v1/callWorkflow","parameters":{}}],
            "tasks":[{"name":"logger_1","type":"logger/v1/info","parameters":{"text":"hello"}}]}
            """;

        return workflowService.create(
            definition.replace("LABEL", label), Workflow.Format.JSON, Workflow.SourceType.JDBC);
    }

    public Workspace createWorkspace(String name) {
        return workspaceRepository.save(new Workspace(name));
    }

    public WorkspaceUser createWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        return workspaceUserRepository.save(new WorkspaceUser(userId, workspaceId, workspaceRole.ordinal()));
    }

    public int publishProject(long projectId) {
        return projectService.publishProject(projectId, "published", false);
    }
}
