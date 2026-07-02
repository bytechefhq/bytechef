/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * Owns the transactional persistence of connected-user project workflows. Keeping these writes in a dedicated bean lets
 * {@link ConnectedUserProjectFacadeImpl}'s AI-generation methods run with {@code Propagation.NOT_SUPPORTED} yet still
 * commit the skeleton workflow through a normal Spring proxy hop before generation begins — which removes the facade's
 * self-injection.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserProjectWorkflowManager {

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [],
            "triggers": [],
            "tasks": []
        }
        """;
    private static final String MARKER = "__EMBEDDED__";

    private final ConnectedUserProjectService connectUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final ProjectService projectService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectWorkflowManager(
        ConnectedUserProjectService connectUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserService connectedUserService, ConnectionService connectionService, ProjectService projectService,
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService, WorkflowTestConfigurationFacade workflowTestConfigurationFacade) {

        this.connectUserProjectService = connectUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
    }

    public String createProjectWorkflow(String externalUserId, String definition, Environment environment) {
        ConnectedUserProject connectedUserProject = getOrCreateConnectedUserProject(externalUserId, environment);

        String effectiveDefinition = StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition;

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            connectedUserProject.getProjectId(), effectiveDefinition);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = new ConnectedUserProjectWorkflow();

        connectedUserProjectWorkflow.setConnectedUserProjectId(connectedUserProject.getId());
        connectedUserProjectWorkflow.setProjectWorkflowId(projectWorkflow.getId());

        connectedUserProjectWorkflowService.create(connectedUserProjectWorkflow);

        List<Connection> connections = connectionService.getConnections(PlatformType.EMBEDDED);
        Map<String, ?> workflowMap = JsonUtils.readMap(effectiveDefinition);

        checkWorkflowNodeConnections(workflowMap, connections, projectWorkflow, environment.ordinal());

        return projectWorkflow.getUuidAsString();
    }

    public void updateProjectWorkflow(
        String externalUserId, String workflowUuid, String definition, Environment environment) {

        ConnectedUserProject connectedUserProject = getOrCreateConnectedUserProject(externalUserId, environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getLastProjectWorkflow(
            connectedUserProject.getProjectId(), workflowUuid);

        Workflow oldWorkflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

        workflowService.update(projectWorkflow.getWorkflowId(), definition, oldWorkflow.getVersion());
    }

    public ConnectedUserProject getOrCreateConnectedUserProject(String externalUserId, Environment environment) {
        return connectUserProjectService
            .fetchConnectUserProject(externalUserId, environment)
            .orElseGet(() -> {
                Project project = new Project();

                project.setName(MARKER + externalUserId);
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
}
