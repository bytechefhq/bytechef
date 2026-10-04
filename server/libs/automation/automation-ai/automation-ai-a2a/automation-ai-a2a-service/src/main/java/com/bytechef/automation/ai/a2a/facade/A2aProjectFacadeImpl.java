/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.ai.a2a.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.a2a.domain.A2aProject;
import com.bytechef.automation.ai.a2a.domain.A2aProjectWorkflow;
import com.bytechef.automation.ai.a2a.domain.A2aServer;
import com.bytechef.automation.ai.a2a.service.A2aProjectService;
import com.bytechef.automation.ai.a2a.service.A2aProjectWorkflowService;
import com.bytechef.automation.ai.a2a.service.A2aServerService;
import com.bytechef.automation.ai.a2a.util.A2aWorkflowTriggerUtils;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class A2aProjectFacadeImpl implements A2aProjectFacade {

    private final A2aProjectService a2aProjectService;
    private final A2aProjectWorkflowService a2aProjectWorkflowService;
    private final A2aServerService a2aServerService;
    private final ComponentConnectionFacade componentConnectionFacade;
    private final ConnectionService connectionService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public A2aProjectFacadeImpl(
        A2aProjectService a2aProjectService, A2aProjectWorkflowService a2aProjectWorkflowService,
        A2aServerService a2aServerService, ComponentConnectionFacade componentConnectionFacade,
        ConnectionService connectionService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.a2aProjectService = a2aProjectService;
        this.a2aProjectWorkflowService = a2aProjectWorkflowService;
        this.a2aServerService = a2aServerService;
        this.componentConnectionFacade = componentConnectionFacade;
        this.connectionService = connectionService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProject createA2aProject(
        long a2aServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {

        validateProjectVersionPublished(projectId, projectVersion);

        Set<String> workflowIds = validateWorkflowIds(projectId, projectVersion, selectedWorkflowIds);

        A2aServer a2aServer = a2aServerService.getA2aServer(a2aServerId);

        validateProjectNotAttached(a2aServerId, projectId);

        Map<String, List<ProjectDeploymentWorkflowConnection>> workflowConnectionsMap = resolveConnections(
            projectId, workflowIds, a2aServer.getEnvironment());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName(SystemProjects.A2A_SERVER_DEPLOYMENT_NAME_PREFIX + projectId + "_v" + projectVersion);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(a2aServer.getEnvironment());

        projectDeployment = projectDeploymentService.create(projectDeployment);

        projectDeploymentService.updateEnabled(projectDeployment.getId(), true);

        A2aProject a2aProject = a2aProjectService.create(projectDeployment.getId(), a2aServerId, projectId);

        for (Map.Entry<String, List<ProjectDeploymentWorkflowConnection>> entry : workflowConnectionsMap.entrySet()) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
                projectDeployment.getId(), entry.getKey(), entry.getValue());

            a2aProjectWorkflowService.create(a2aProject.getId(), projectDeploymentWorkflow.getId());
        }

        return a2aProject;
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void deleteA2aProject(long a2aProjectId) {
        A2aProject a2aProject = a2aProjectService.fetchA2aProject(a2aProjectId)
            .orElseThrow(() -> new IllegalArgumentException("A2aProject not found: " + a2aProjectId));

        projectDeploymentFacade.deleteProjectDeployment(a2aProject.getProjectDeploymentId());
    }

    @Override
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public A2aProject updateA2aProject(long a2aProjectId, List<String> selectedWorkflowIds) {
        A2aProject a2aProject = a2aProjectService.fetchA2aProject(a2aProjectId)
            .orElseThrow(() -> new IllegalArgumentException("A2aProject not found: " + a2aProjectId));

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            a2aProject.getProjectDeploymentId());

        Set<String> selectedWorkflowIdSet = validateWorkflowIds(
            projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), selectedWorkflowIds);

        Map<Long, ProjectDeploymentWorkflow> projectDeploymentWorkflowMap = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(a2aProject.getProjectDeploymentId())) {

            projectDeploymentWorkflowMap.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);
        }

        Map<String, A2aProjectWorkflow> existingWorkflowIdMap = new HashMap<>();

        for (A2aProjectWorkflow a2aProjectWorkflow : a2aProjectWorkflowService
            .getA2aProjectA2aProjectWorkflows(a2aProjectId)) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowMap.get(
                a2aProjectWorkflow.getProjectDeploymentWorkflowId());

            if (projectDeploymentWorkflow != null) {
                existingWorkflowIdMap.put(projectDeploymentWorkflow.getWorkflowId(), a2aProjectWorkflow);
            }
        }

        Set<String> addedWorkflowIds = new LinkedHashSet<>(selectedWorkflowIdSet);

        addedWorkflowIds.removeAll(existingWorkflowIdMap.keySet());

        Map<String, List<ProjectDeploymentWorkflowConnection>> addedWorkflowConnectionsMap = resolveConnections(
            projectDeployment.getProjectId(), addedWorkflowIds, projectDeployment.getEnvironment());

        for (Map.Entry<String, List<ProjectDeploymentWorkflowConnection>> entry : addedWorkflowConnectionsMap
            .entrySet()) {

            ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
                a2aProject.getProjectDeploymentId(), entry.getKey(), entry.getValue());

            a2aProjectWorkflowService.create(a2aProjectId, projectDeploymentWorkflow.getId());
        }

        for (Map.Entry<String, A2aProjectWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                A2aProjectWorkflow a2aProjectWorkflow = entry.getValue();

                ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowMap.get(
                    a2aProjectWorkflow.getProjectDeploymentWorkflowId());

                if (projectDeploymentWorkflow.isEnabled()) {
                    projectDeploymentFacade.enableProjectDeploymentWorkflow(
                        a2aProject.getProjectDeploymentId(), entry.getKey(), false);
                }

                a2aProjectWorkflowService.delete(a2aProjectWorkflow.getId());

                projectDeploymentWorkflowService.delete(a2aProjectWorkflow.getProjectDeploymentWorkflowId());
            }
        }

        return a2aProject;
    }

    private void validateProjectNotAttached(long a2aServerId, long projectId) {
        for (A2aProject a2aProject : a2aProjectService.getA2aServerA2aProjects(a2aServerId)) {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                a2aProject.getProjectDeploymentId());

            if (Objects.equals(projectDeployment.getProjectId(), projectId)) {
                throw new IllegalArgumentException(
                    "Project " + projectId + " is already attached to A2A server " + a2aServerId);
            }
        }
    }

    private void validateProjectVersionPublished(long projectId, int projectVersion) {
        Project project = projectService.getProject(projectId);

        if (!project.isPublished()) {
            throw new IllegalArgumentException("Project " + projectId + " is not published");
        }

        boolean projectVersionPublished = project.getProjectVersions()
            .stream()
            .anyMatch(curProjectVersion -> curProjectVersion.getVersion() == projectVersion &&
                curProjectVersion.getStatus() == ProjectVersion.Status.PUBLISHED);

        if (!projectVersionPublished) {
            throw new IllegalArgumentException(
                "Version " + projectVersion + " of project " + projectId + " is not published");
        }
    }

    private Set<String> validateWorkflowIds(long projectId, int projectVersion, List<String> selectedWorkflowIds) {
        List<String> projectWorkflowIds = projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion);

        Set<String> workflowIds = new LinkedHashSet<>(selectedWorkflowIds);

        for (String workflowId : workflowIds) {
            if (!projectWorkflowIds.contains(workflowId)) {
                throw new IllegalArgumentException(
                    "Workflow " + workflowId + " does not belong to version " + projectVersion + " of project " +
                        projectId);
            }

            Workflow workflow = workflowService.getWorkflow(workflowId);

            if (A2aWorkflowTriggerUtils.fetchNewWorkflowCallTrigger(workflow)
                .isEmpty()) {

                throw new IllegalArgumentException(
                    "Workflow " + workflowId + " has no New Workflow Call trigger and cannot be exposed as an A2A " +
                        "skill");
            }
        }

        return workflowIds;
    }

    private List<ComponentConnection> getComponentConnections(Workflow workflow) {
        List<ComponentConnection> componentConnections = new ArrayList<>();

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            componentConnections.addAll(componentConnectionFacade.getComponentConnections(workflowTrigger));
        }

        for (WorkflowTask workflowTask : workflow.getTasks(true)) {
            componentConnections.addAll(componentConnectionFacade.getComponentConnections(workflowTask));
        }

        return componentConnections;
    }

    private List<WorkflowTestConfigurationConnection> getWorkflowTestConfigurationConnections(
        long projectId, String workflowId, Environment environment) {

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        String lastWorkflowId = projectWorkflowService
            .fetchLastProjectWorkflowId(projectId, projectWorkflow.getUuidAsString())
            .orElse(workflowId);

        return workflowTestConfigurationService.fetchWorkflowTestConfiguration(lastWorkflowId, environment.ordinal())
            .map(WorkflowTestConfiguration::getConnections)
            .orElse(List.of());
    }

    private boolean isConnectionUsable(
        long connectionId, ComponentConnection componentConnection, Environment environment) {

        Connection connection = connectionService.getConnection(connectionId);

        return connection.getEnvironmentId() == environment.ordinal() &&
            Objects.equals(connection.getComponentName(), componentConnection.componentName());
    }

    private static boolean isSameConnectionSlot(
        WorkflowTestConfigurationConnection workflowTestConfigurationConnection,
        ComponentConnection componentConnection) {

        return Objects.equals(
            workflowTestConfigurationConnection.getWorkflowNodeName(), componentConnection.workflowNodeName()) &&
            Objects.equals(workflowTestConfigurationConnection.getWorkflowConnectionKey(), componentConnection.key());
    }

    private List<ProjectDeploymentWorkflowConnection> resolveConnections(
        long projectId, String workflowId, Environment environment) {

        List<ComponentConnection> componentConnections = getComponentConnections(
            workflowService.getWorkflow(workflowId));

        if (componentConnections.isEmpty()) {
            return List.of();
        }

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            getWorkflowTestConfigurationConnections(projectId, workflowId, environment);

        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections = new ArrayList<>();

        for (ComponentConnection componentConnection : componentConnections) {
            Optional<Long> connectionId = workflowTestConfigurationConnections.stream()
                .filter(workflowTestConfigurationConnection -> isSameConnectionSlot(
                    workflowTestConfigurationConnection, componentConnection))
                .map(WorkflowTestConfigurationConnection::getConnectionId)
                .filter(Objects::nonNull)
                .filter(curConnectionId -> isConnectionUsable(curConnectionId, componentConnection, environment))
                .findFirst();

            if (connectionId.isPresent()) {
                projectDeploymentWorkflowConnections.add(
                    new ProjectDeploymentWorkflowConnection(
                        connectionId.get(), componentConnection.key(), componentConnection.workflowNodeName()));
            } else if (componentConnection.required()) {
                throw new IllegalArgumentException(
                    "Workflow " + workflowId + " requires a " + componentConnection.componentName() +
                        " connection for " + componentConnection.workflowNodeName() + " in the " + environment +
                        " environment; select one in the workflow editor before exposing it as an A2A skill");
            }
        }

        return projectDeploymentWorkflowConnections;
    }

    private Map<String, List<ProjectDeploymentWorkflowConnection>> resolveConnections(
        long projectId, Set<String> workflowIds, Environment environment) {

        Map<String, List<ProjectDeploymentWorkflowConnection>> workflowConnectionsMap = new LinkedHashMap<>();

        for (String workflowId : workflowIds) {
            workflowConnectionsMap.put(workflowId, resolveConnections(projectId, workflowId, environment));
        }

        return workflowConnectionsMap;
    }

    private ProjectDeploymentWorkflow createProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowId, List<ProjectDeploymentWorkflowConnection> connections) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(Map.of());
        projectDeploymentWorkflow.setConnections(connections);

        return projectDeploymentWorkflowService.create(projectDeploymentWorkflow);
    }
}
