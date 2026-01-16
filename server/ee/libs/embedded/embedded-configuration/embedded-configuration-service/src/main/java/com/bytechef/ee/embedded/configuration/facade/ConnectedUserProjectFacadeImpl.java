/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflowConnection;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ConnectedUserProjectFacadeImpl implements ConnectedUserProjectFacade {

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [],
            "triggers": [],
            "tasks": []
        }
        """;

    private final ConnectedUserProjectService connectUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final EnvironmentService environmentService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectFacade projectFacade;
    private final ProjectService projectService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectFacadeImpl(
        ConnectedUserProjectService connectUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        EnvironmentService environmentService, JobService jobService, PrincipalJobService principalJobService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectFacade projectFacade,
        ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowService projectWorkflowService, WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.connectUserProjectService = connectUserProjectService;
        this.connectedUserService = connectedUserService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectionService = connectionService;
        this.environmentService = environmentService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectFacade = projectFacade;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public String createProjectWorkflow(String externalUserId, String definition, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            connectedUserProject.getProjectId(), StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProject.getId());
        connectedUserProjectWorkflow.setProjectWorkflowId(projectWorkflow.getId());

        connectedUserProjectWorkflowService.create(connectedUserProjectWorkflow);

        List<Connection> connections = connectionService.getConnections(PlatformType.EMBEDDED);
        Map<String, ?> workflowMap = JsonUtils.readMap(definition);

        checkWorkflowNodeConnections(workflowMap, connections, projectWorkflow, environment.ordinal());

        return projectWorkflow.getUuidAsString();
    }

    @Override
    public void deleteProjectWorkflow(String externalUserId, String workflowUuid, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            connectedUserProject.getProjectId(), workflowUuid);

        Set<Long> connectionIds = new HashSet<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            connectedUserProjectWorkflowService
                .fetchConnectedUserProjectWorkflow(connectedUserProject.getId(), projectWorkflow.getId())
                .ifPresent(connectedUserProjectWorkflow -> {
                    connectionIds.addAll(
                        connectedUserProjectWorkflow.getConnections()
                            .stream()
                            .map(ConnectedUserProjectWorkflowConnection::getConnectionId)
                            .toList());

                    connectedUserProjectWorkflowService.delete(connectedUserProjectWorkflow.getId());
                });

            projectWorkflowFacade.deleteWorkflow(projectWorkflow.getWorkflowId());
        }

        for (Long connectionId : connectionIds) {
            connectionService.delete(connectionId);
        }
    }

    @Override
    public void enableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, Long environmentId) {

        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        String workflowId = projectWorkflowService
            .getProjectWorkflowWorkflowId(
                projectDeploymentService.getProjectDeploymentId(connectedUserProject.getProjectId(), environment),
                workflowUuid);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            connectedUserProject.getProjectId(), workflowId, enable, environment);
    }

    @Override
    public ConnectedUserProjectWorkflowDTO getConnectedUserProjectWorkflow(
        String externalUserId, String workflowUuid, Long environmentId) {

        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getLastProjectWorkflow(
            connectedUserProject.getProjectId(), workflowUuid);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflow(connectedUserProject.getId(), projectWorkflow.getId());

        return new ConnectedUserProjectWorkflowDTO(
            connectedUserProject.getConnectedUserId(), connectedUserProjectWorkflow,
            isProjectDeploymentWorkflowEnabled(projectWorkflow, environment),
            getWorkflowLastExecutionDate(projectWorkflow.getWorkflowId()), projectWorkflow,
            workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()));
    }

    @Override
    public List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        String externalUserId, Environment environment) {

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        return getConnectedUserProjectWorkflows(connectedUserProject, environment);
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectUserProjectService.getConnectedUserProjects(connectedUserId, environment)
            .stream()
            .filter(connectedUserProject -> {
                if (connectedUserId != null) {
                    return Objects.equals(connectedUserProject.getConnectedUserId(), connectedUserId);
                }

                return true;
            })
            .filter(connectedUserProject -> {
                if (environment != null) {
                    return projectDeploymentService
                        .fetchProjectDeployment(connectedUserProject.getProjectId(), environment)
                        .map(projectDeployment -> projectDeployment.getEnvironment() == environment)
                        .orElse(false);
                }

                return true;
            })
            .map(connectedUserProject -> {
                ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                    connectedUserProject.getConnectedUserId());

                List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows =
                    getConnectedUserProjectWorkflows(connectedUserProject, connectedUser.getEnvironment());

                ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                    connectedUserProject.getProjectId(), connectedUser.getEnvironment());

                return new ConnectedUserProjectDTO(
                    connectedUserProject, connectedUser, connectedUserProjectWorkflows,
                    getProjectDeploymentLastExecutionDate(projectDeployment.getId()), projectDeployment);
            })
            .toList();
    }

    @Override
    public void publishProjectWorkflow(
        String externalUserId, String workflowUuid, String description, Long environmentId) {

        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        String workflowId = projectWorkflowService
            .fetchLastProjectWorkflowId(connectedUserProject.getProjectId(), workflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowUuid: %s not exist".formatted(workflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        List<ProjectDeploymentWorkflowConnection> connections = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, environment.ordinal())
            .map(WorkflowTestConfiguration::getConnections)
            .orElse(List.of())
            .stream()
            .filter(workflowTestConfigurationConnection -> {
                Connection connection = connectionService.getConnection(
                    workflowTestConfigurationConnection.getConnectionId());

                return connection.getEnvironmentId() == environment.ordinal();
            })
            .map(workflowTestConfigurationConnection -> new ProjectDeploymentWorkflowConnection(
                workflowTestConfigurationConnection.getConnectionId(),
                workflowTestConfigurationConnection.getWorkflowConnectionKey(),
                workflowTestConfigurationConnection.getWorkflowNodeName()))
            .toList();

        int newProjectVersion = projectFacade.publishProject(connectedUserProject.getProjectId(), description, false);

        if (newProjectVersion == 2) {
            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setEnabled(true);
            projectDeployment.setEnvironment(environment);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                connectedUserProject.getConnectedUserId());

            projectDeployment.setName("__EMBEDDED__" + connectedUser.getExternalId());

            projectDeployment.setProjectId(connectedUserProject.getProjectId());
            projectDeployment.setProjectVersion(1);

            projectDeploymentFacade.createProjectDeployment(projectDeployment, workflowId, connections);
        } else {
            projectDeploymentFacade.updateProjectDeployment(
                connectedUserProject.getProjectId(), newProjectVersion - 1, workflowUuid, connections,
                environmentId);
        }

        connectedUserProjectWorkflowService.incrementWorkflowVersion(
            connectedUserProject.getId(), projectWorkflow.getId());
    }

    @Override
    public void updateProjectWorkflow(
        String externalUserId, String workflowUuid, String definition, Environment environment) {

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getLastProjectWorkflow(
            connectedUserProject.getProjectId(), workflowUuid);

        Workflow oldWorkflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

        workflowService.update(projectWorkflow.getWorkflowId(), definition, oldWorkflow.getVersion());
    }

    @Override
    public void updateWorkflowConfigurationConnection(
        String externalUserId, String workflowUuid, String workflowNodeName, String workflowConnectionKey,
        long connectionId, Environment environment) {

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(externalUserId, environment);

        String workflowId = projectWorkflowService
            .fetchLastProjectWorkflowId(connectedUserProject.getProjectId(), workflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowUuid: %s not exist".formatted(workflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey, connectionId, environment.ordinal());
    }

    private ConnectedUserProject checkConnectedUserProject(String externalUserId, Environment environment) {
        return connectUserProjectService
            .fetchConnectUserProject(externalUserId, environment)
            .orElseGet(() -> {
                Project project = new Project();

                project.setName("__EMBEDDED__" + externalUserId);
                project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

                project = projectService.create(project);

                ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

                return connectUserProjectService.create(connectedUser.getId(), project.getId());
            });
    }

    private void checkWorkflowNodeConnection(
        String map, List<Connection> connections, ProjectWorkflow projectWorkflow, String workflowNodeName,
        long environmentId) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(map);

        connections.stream()
            .filter(connection -> Objects.equals(connection.getComponentName(), workflowNodeType.name()))
            .findFirst()
            .ifPresent(connection -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
                projectWorkflow.getWorkflowId(), workflowNodeName, workflowNodeType.name(),
                connection.getId(), environmentId));
    }

    private void checkWorkflowNodeConnections(
        Map<String, ?> workflowMap, List<Connection> connections, ProjectWorkflow projectWorkflow, long environmentId) {

        List<Map<String, ?>> triggers = MapUtils.getList(workflowMap, "triggers", new TypeReference<>() {}, List.of());

        for (Map<String, ?> triggerMap : triggers) {
            checkWorkflowNodeConnection(
                MapUtils.getString(triggerMap, "type"), connections, projectWorkflow,
                MapUtils.getString(triggerMap, "name"), environmentId);
        }

        List<Map<String, ?>> tasks = MapUtils.getList(workflowMap, "tasks", new TypeReference<>() {}, List.of());

        for (Map<String, ?> taskMap : tasks) {
            checkWorkflowNodeConnection(
                MapUtils.getString(taskMap, "type"), connections, projectWorkflow, MapUtils.getString(taskMap, "name"),
                environmentId);
        }
    }

    private List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        ConnectedUserProject connectedUserProject, Environment environment) {

        Project project = projectService.getProject(connectedUserProject.getProjectId());

        List<ProjectWorkflow> latestProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion());

        List<String> latestWorkflowIds = latestProjectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        return workflowService.getWorkflows(latestWorkflowIds)
            .stream()
            .map(workflow -> {
                ProjectWorkflow latestProjectWorkflow = latestProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(curProjectWorkflow.getWorkflowId(), workflow.getId()))
                    .findFirst()
                    .orElseThrow();

                ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
                    .getConnectedUserProjectWorkflow(connectedUserProject.getId(), latestProjectWorkflow.getId());

                return new ConnectedUserProjectWorkflowDTO(
                    connectedUserProject.getConnectedUserId(), connectedUserProjectWorkflow,
                    isProjectDeploymentWorkflowEnabled(latestProjectWorkflow, environment),
                    getWorkflowLastExecutionDate(latestProjectWorkflow.getWorkflowId()), latestProjectWorkflow,
                    new WorkflowDTO(workflow, List.of(), List.of()));
            })
            .toList();
    }

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private Instant getProjectDeploymentLastExecutionDate(long projectDeploymentId) {
        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastJobId(projectDeploymentId, PlatformType.AUTOMATION), this::getJobEndDate,
            null);
    }

    private Instant getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(workflowId), Job::getEndDate, null);
    }

    private boolean isProjectDeploymentWorkflowEnabled(ProjectWorkflow projectWorkflow, Environment environment) {
        // DRAFT status
        if (projectWorkflow.getProjectVersion() == 1) {
            return false;
        }

        projectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(
                projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion() - 1,
                projectWorkflow.getUuidAsString())
            .orElse(null);

        if (projectWorkflow == null) {
            return false;
        }

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            projectWorkflow.getProjectId(), environment);

        return projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, projectWorkflow.getWorkflowId());
    }
}
