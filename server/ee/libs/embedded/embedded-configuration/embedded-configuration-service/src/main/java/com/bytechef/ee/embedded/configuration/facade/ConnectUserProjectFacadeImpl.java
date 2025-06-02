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
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.constant.ModeType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.security.util.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectUserProjectFacadeImpl implements ConnectUserProjectFacade {

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
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectFacade projectFacade;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ConnectUserProjectFacadeImpl(
        ConnectedUserProjectService connectUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectFacade projectFacade, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade,
        ProjectDeploymentService projectDeploymentService) {

        this.connectUserProjectService = connectUserProjectService;
        this.connectedUserService = connectedUserService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectionService = connectionService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectFacade = projectFacade;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public String createProjectWorkflow(String definition, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        ProjectWorkflow projectWorkflow = projectFacade.addWorkflow(
            connectedUserProject.getProjectId(), StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProject.getId());
        connectedUserProjectWorkflow.setProjectWorkflowId(projectWorkflow.getId());

        connectedUserProjectWorkflowService.create(connectedUserProjectWorkflow);

        List<Connection> connections = connectionService.getConnections(ModeType.EMBEDDED);
        Map<String, ?> workflowMap = JsonUtils.readMap(definition);

        checkWorkflowNodeConnections(workflowMap, connections, projectWorkflow);

        return projectWorkflow.getWorkflowReferenceCode();
    }

    @Override
    public void deleteProjectWorkflow(String workflowReferenceCode, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            connectedUserProject.getProjectId(), workflowReferenceCode);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            projectFacade.deleteWorkflow(projectWorkflow.getWorkflowId());
        }
    }

    @Override
    public void enableProjectWorkflow(String workflowReferenceCode, boolean enable, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        String workflowId = projectWorkflowService
            .fetchProjectWorkflowId(connectedUserProject.getProjectId(), workflowReferenceCode)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowReferenceCode: %s not exist".formatted(workflowReferenceCode),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            connectedUserProject.getProjectId(), environment);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enable);
    }

    @Override
    public ConnectUserProjectWorkflowDTO getProjectWorkflow(String workflowReferenceCode, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            connectedUserProject.getProjectId(), workflowReferenceCode);

        return new ConnectUserProjectWorkflowDTO(
            connectedUserProject.getConnectedUserId(), projectWorkflow,
            workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()));
    }

    @Override
    public List<ConnectUserProjectWorkflowDTO> getProjectWorkflows(Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        Project project = projectService.getProject(connectedUserProject.getProjectId());
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(project.getId());

        List<String> workflowIds = projectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        return workflowService.getWorkflows(workflowIds)
            .stream()
            .map(workflow -> {
                ProjectWorkflow projectWorkflow = projectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(curProjectWorkflow.getWorkflowId(), workflow.getId()))
                    .findFirst()
                    .orElseThrow();

                return new ConnectUserProjectWorkflowDTO(
                    connectedUserProject.getConnectedUserId(),
                    projectWorkflow, new WorkflowDTO(workflow, List.of(), List.of()));
            })
            .toList();
    }

    @Override
    public void publishProjectWorkflow(String workflowReferenceCode, String description, Environment environment) {
        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        String workflowId = projectWorkflowService
            .fetchProjectWorkflowId(connectedUserProject.getProjectId(), workflowReferenceCode)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowReferenceCode: %s not exist".formatted(workflowReferenceCode),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        int newProjectVersion = projectFacade.publishProject(connectedUserProject.getProjectId(), description, false);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setInputs(Map.of());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        if (newProjectVersion == 2) {
            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setEnabled(true);
            projectDeployment.setEnvironment(environment);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                connectedUserProject.getConnectedUserId());

            projectDeployment.setName("EMBEDDED_" + connectedUser.getExternalId());

            projectDeployment.setProjectId(connectedUserProject.getProjectId());
            projectDeployment.setProjectVersion(1);

            projectDeploymentFacade.createProjectDeployment(
                projectDeployment, List.of(projectDeploymentWorkflow), List.of());
        } else {
            long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
                connectedUserProject.getProjectId(), environment);

            projectDeploymentFacade.updateProjectDeployment(projectDeploymentId, List.of(projectDeploymentWorkflow));
        }
    }

    @Override
    public void updateProjectWorkflow(
        String workflowReferenceCode, String definition, Environment environment) {

        ConnectedUserProject connectedUserProject = checkConnectedUserProject(
            OptionalUtils.get(SecurityUtils.getCurrentUserLogin(), "User not found"), environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            connectedUserProject.getProjectId(), workflowReferenceCode);

        Workflow oldWorkflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

        workflowService.update(projectWorkflow.getWorkflowId(), definition, oldWorkflow.getVersion());
    }

    private ConnectedUserProject checkConnectedUserProject(String externalUserId, Environment environment) {
        return connectUserProjectService
            .fetchConnectUserProject(externalUserId, environment)
            .orElseGet(() -> {
                Project project = new Project();

                project.setName("EMBEDDED_" + externalUserId);
                project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);

                project = projectService.create(project);

                ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

                return connectUserProjectService.create(connectedUser.getId(), project.getId());
            });
    }

    private void checkWorkflowNodeConnection(
        String map, List<Connection> connections, ProjectWorkflow projectWorkflow, String workflowNodeName) {

        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(map);

        connections.stream()
            .filter(connection -> Objects.equals(connection.getComponentName(), workflowNodeType.name()))
            .findFirst()
            .ifPresent(connection -> workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
                projectWorkflow.getWorkflowId(), workflowNodeName, workflowNodeType.name(),
                connection.getId()));
    }

    private void checkWorkflowNodeConnections(
        Map<String, ?> workflowMap, List<Connection> connections, ProjectWorkflow projectWorkflow) {

        List<Map<String, ?>> triggers = MapUtils.getList(workflowMap, "triggers", new TypeReference<>() {}, List.of());

        for (Map<String, ?> triggerMap : triggers) {
            checkWorkflowNodeConnection(
                MapUtils.getString(triggerMap, "type"), connections, projectWorkflow,
                MapUtils.getString(triggerMap, "name"));
        }

        List<Map<String, ?>> tasks = MapUtils.getList(workflowMap, "tasks", new TypeReference<>() {}, List.of());

        for (Map<String, ?> taskMap : tasks) {
            checkWorkflowNodeConnection(
                MapUtils.getString(taskMap, "type"), connections, projectWorkflow, MapUtils.getString(taskMap, "name"));
        }
    }
}
