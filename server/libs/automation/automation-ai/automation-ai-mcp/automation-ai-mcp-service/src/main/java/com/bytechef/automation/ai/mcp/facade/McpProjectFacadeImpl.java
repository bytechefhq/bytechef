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

package com.bytechef.automation.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditEvent;
import com.bytechef.automation.ai.mcp.audit.McpProjectAuditPublisher;
import com.bytechef.automation.ai.mcp.domain.McpProject;
import com.bytechef.automation.ai.mcp.domain.McpProjectWorkflow;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.ai.mcp.util.McpWorkflowUtils;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
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
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO.WorkflowConnectionDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class McpProjectFacadeImpl implements McpProjectFacade {

    private final ComponentConnectionFacade componentConnectionFacade;
    private final ConnectionService connectionService;
    private final McpProjectAuditPublisher mcpProjectAuditPublisher;
    private final McpProjectService mcpProjectService;
    private final McpProjectWorkflowService mcpProjectWorkflowService;
    private final McpServerService mcpServerService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkspaceMcpServerService workspaceMcpServerService;

    @SuppressFBWarnings("EI")
    public McpProjectFacadeImpl(
        ComponentConnectionFacade componentConnectionFacade, ConnectionService connectionService,
        McpProjectAuditPublisher mcpProjectAuditPublisher, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService, McpServerService mcpServerService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceMcpServerService workspaceMcpServerService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.connectionService = connectionService;
        this.mcpProjectAuditPublisher = mcpProjectAuditPublisher;
        this.mcpProjectService = mcpProjectService;
        this.mcpProjectWorkflowService = mcpProjectWorkflowService;
        this.mcpServerService = mcpServerService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workspaceMcpServerService = workspaceMcpServerService;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and " +
        "hasPermission(#projectId, 'Project', 'DEPLOYMENT_CREATE')")
    public McpProject createMcpProject(
        long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds) {

        return createMcpProject(mcpServerId, projectId, projectVersion, selectedWorkflowIds, List.of());
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and " +
        "hasPermission(#projectId, 'Project', 'DEPLOYMENT_CREATE')")
    public McpProject createMcpProject(
        long mcpServerId, long projectId, int projectVersion, List<String> selectedWorkflowIds,
        List<WorkflowConfigurationDTO> workflowConfigurations) {

        Project project = projectService.getProject(projectId);

        validateSameWorkspace(project, mcpServerId);

        validateProjectVersionPublished(project, projectVersion);

        validateProjectVersionWorkflowIds(projectId, projectVersion, selectedWorkflowIds);

        validateCallableWorkflows(selectedWorkflowIds);

        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        Map<String, WorkflowDeploymentSettings> workflowDeploymentSettingsMap = resolveWorkflowDeploymentSettings(
            projectId, selectedWorkflowIds, workflowConfigurations, mcpServer.getEnvironment());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setName(McpServer.MCP_SERVER_NAME_PREFIX + projectId + "_v" + projectVersion);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setEnvironment(mcpServer.getEnvironment());

        projectDeployment = projectDeploymentService.create(projectDeployment);

        projectDeploymentService.updateEnabled(projectDeployment.getId(), true);

        McpProject mcpProject = new McpProject(projectDeployment.getId(), mcpServerId);

        mcpProject = mcpProjectService.create(mcpProject);

        for (Map.Entry<String, WorkflowDeploymentSettings> entry : workflowDeploymentSettingsMap.entrySet()) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
                projectDeployment.getId(), entry.getKey(), entry.getValue());

            mcpProjectWorkflowService.create(mcpProject.getId(), projectDeploymentWorkflow.getId());
        }

        Map<String, Object> data = new HashMap<>();

        data.put("projectId", String.valueOf(projectId));

        mcpProjectAuditPublisher.publish(McpProjectAuditEvent.MCP_PROJECT_CREATED, mcpProject.getId(), data);

        return mcpProject;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public void deleteMcpProject(long mcpProjectId) {
        McpProject mcpProject = mcpProjectService.fetchMcpProject(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException("McpProject not found: " + mcpProjectId));

        projectDeploymentFacade.deleteProjectDeployment(Objects.requireNonNull(mcpProject.getProjectDeploymentId()));

        mcpProjectAuditPublisher.publish(McpProjectAuditEvent.MCP_PROJECT_DELETED, mcpProjectId);
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProject updateMcpProject(long mcpProjectId, List<String> selectedWorkflowIds) {
        return updateMcpProject(mcpProjectId, selectedWorkflowIds, List.of());
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT')")
    public McpProject updateMcpProject(
        long mcpProjectId, List<String> selectedWorkflowIds, List<WorkflowConfigurationDTO> workflowConfigurations) {

        McpProject mcpProject = mcpProjectService.fetchMcpProject(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException("McpProject not found: " + mcpProjectId));

        List<McpProjectWorkflow> existingMcpProjectWorkflows =
            mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(mcpProject.getProjectDeploymentId());

        Map<Long, ProjectDeploymentWorkflow> projectDeploymentWorkflowMap = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            projectDeploymentWorkflowMap.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);
        }

        Map<String, McpProjectWorkflow> existingWorkflowIdMap = new HashMap<>();

        for (McpProjectWorkflow mcpProjectWorkflow : existingMcpProjectWorkflows) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                projectDeploymentWorkflowMap.get(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            if (projectDeploymentWorkflow != null) {
                existingWorkflowIdMap.put(projectDeploymentWorkflow.getWorkflowId(), mcpProjectWorkflow);
            }
        }

        List<String> addedWorkflowIds = selectedWorkflowIds.stream()
            .filter(workflowId -> !existingWorkflowIdMap.containsKey(workflowId))
            .toList();

        Map<String, WorkflowDeploymentSettings> addedWorkflowDeploymentSettingsMap = Map.of();

        Set<String> selectedWorkflowIdSet = new HashSet<>(selectedWorkflowIds);

        List<WorkflowConfigurationDTO> keptWorkflowConfigurations = workflowConfigurations.stream()
            .filter(workflowConfiguration -> existingWorkflowIdMap.containsKey(workflowConfiguration.workflowId()) &&
                selectedWorkflowIdSet.contains(workflowConfiguration.workflowId()))
            .toList();

        if (!addedWorkflowIds.isEmpty() || !keptWorkflowConfigurations.isEmpty()) {
            ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                Objects.requireNonNull(mcpProject.getProjectDeploymentId()));

            if (!addedWorkflowIds.isEmpty()) {
                validateProjectVersionWorkflowIds(
                    projectDeployment.getProjectId(), projectDeployment.getProjectVersion(), addedWorkflowIds);

                validateCallableWorkflows(addedWorkflowIds);

                addedWorkflowDeploymentSettingsMap = resolveWorkflowDeploymentSettings(
                    projectDeployment.getProjectId(), addedWorkflowIds, workflowConfigurations,
                    projectDeployment.getEnvironment());
            }

            for (WorkflowConfigurationDTO workflowConfiguration : keptWorkflowConfigurations) {
                McpProjectWorkflow mcpProjectWorkflow = existingWorkflowIdMap.get(workflowConfiguration.workflowId());

                ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowMap.get(
                    mcpProjectWorkflow.getProjectDeploymentWorkflowId());

                projectDeploymentWorkflow.setConnections(
                    toProjectDeploymentWorkflowConnections(
                        workflowConfiguration.workflowId(), workflowConfiguration.connections(),
                        projectDeployment.getEnvironment()));
                projectDeploymentWorkflow.setInputs(workflowConfiguration.inputs());

                projectDeploymentWorkflowService.update(projectDeploymentWorkflow);
            }
        }

        for (Map.Entry<String, WorkflowDeploymentSettings> entry : addedWorkflowDeploymentSettingsMap.entrySet()) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow = createProjectDeploymentWorkflow(
                mcpProject.getProjectDeploymentId(), entry.getKey(), entry.getValue());

            mcpProjectWorkflowService.create(mcpProjectId, projectDeploymentWorkflow.getId());
        }

        for (Map.Entry<String, McpProjectWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                McpProjectWorkflow mcpProjectWorkflow = entry.getValue();

                ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflowMap.get(
                    mcpProjectWorkflow.getProjectDeploymentWorkflowId());

                if (projectDeploymentWorkflow.isEnabled()) {
                    projectDeploymentFacade.enableProjectDeploymentWorkflow(
                        Objects.requireNonNull(mcpProject.getProjectDeploymentId()), entry.getKey(), false);
                }

                mcpProjectWorkflowService.delete(mcpProjectWorkflow.getId());

                projectDeploymentWorkflowService.delete(mcpProjectWorkflow.getProjectDeploymentWorkflowId());
            }
        }

        return mcpProject;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpProjectId, 'McpProject', 'MCP_EDIT') and " +
        "hasPermission(#targetMcpServerId, 'McpServer', 'MCP_EDIT')")
    public McpProject cloneMcpProject(long mcpProjectId, long targetMcpServerId) {
        McpProject source = mcpProjectService.fetchMcpProject(mcpProjectId)
            .orElseThrow(() -> new IllegalArgumentException("McpProject not found: " + mcpProjectId));

        Long sourceProjectDeploymentId = source.getProjectDeploymentId();

        if (sourceProjectDeploymentId == null) {
            throw new IllegalStateException(
                "Source McpProject " + mcpProjectId + " has no project deployment to clone from");
        }

        ProjectDeployment sourceDeployment = projectDeploymentService.getProjectDeployment(sourceProjectDeploymentId);

        List<McpProjectWorkflow> sourceMcpProjectWorkflows =
            mcpProjectWorkflowService.getMcpProjectMcpProjectWorkflows(mcpProjectId);

        List<ProjectDeploymentWorkflow> sourceDeploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflows(sourceProjectDeploymentId);

        Map<Long, ProjectDeploymentWorkflow> deploymentWorkflowById = new HashMap<>();

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : sourceDeploymentWorkflows) {
            deploymentWorkflowById.put(projectDeploymentWorkflow.getId(), projectDeploymentWorkflow);
        }

        List<String> selectedWorkflowIds = new ArrayList<>(sourceMcpProjectWorkflows.size());

        for (McpProjectWorkflow mcpProjectWorkflow : sourceMcpProjectWorkflows) {
            ProjectDeploymentWorkflow projectDeploymentWorkflow =
                deploymentWorkflowById.get(mcpProjectWorkflow.getProjectDeploymentWorkflowId());

            if (projectDeploymentWorkflow != null) {
                selectedWorkflowIds.add(projectDeploymentWorkflow.getWorkflowId());
            }
        }

        return createMcpProject(
            targetMcpServerId, sourceDeployment.getProjectId(), sourceDeployment.getProjectVersion(),
            selectedWorkflowIds);
    }

    private ProjectDeploymentWorkflow createProjectDeploymentWorkflow(
        long projectDeploymentId, String workflowId, WorkflowDeploymentSettings workflowDeploymentSettings) {

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(workflowDeploymentSettings.connections());
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(workflowDeploymentSettings.inputs());
        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflowService.create(projectDeploymentWorkflow);
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
                        " environment; select one in the workflow editor before exposing it as an MCP tool");
            }
        }

        return projectDeploymentWorkflowConnections;
    }

    private Map<String, WorkflowDeploymentSettings> resolveWorkflowDeploymentSettings(
        long projectId, List<String> workflowIds, List<WorkflowConfigurationDTO> workflowConfigurations,
        Environment environment) {

        Map<String, WorkflowConfigurationDTO> workflowConfigurationMap = new HashMap<>();

        for (WorkflowConfigurationDTO workflowConfiguration : workflowConfigurations) {
            workflowConfigurationMap.put(workflowConfiguration.workflowId(), workflowConfiguration);
        }

        Map<String, WorkflowDeploymentSettings> workflowDeploymentSettingsMap = new LinkedHashMap<>();

        for (String workflowId : workflowIds) {
            WorkflowConfigurationDTO workflowConfiguration = workflowConfigurationMap.get(workflowId);

            if (workflowConfiguration == null) {
                workflowDeploymentSettingsMap.put(
                    workflowId,
                    new WorkflowDeploymentSettings(resolveConnections(projectId, workflowId, environment), Map.of()));
            } else {
                workflowDeploymentSettingsMap.put(
                    workflowId,
                    new WorkflowDeploymentSettings(
                        toProjectDeploymentWorkflowConnections(
                            workflowId, workflowConfiguration.connections(), environment),
                        workflowConfiguration.inputs()));
            }
        }

        return workflowDeploymentSettingsMap;
    }

    private List<ProjectDeploymentWorkflowConnection> toProjectDeploymentWorkflowConnections(
        String workflowId, List<WorkflowConnectionDTO> workflowConnections, Environment environment) {

        List<ProjectDeploymentWorkflowConnection> projectDeploymentWorkflowConnections = new ArrayList<>();

        for (ComponentConnection componentConnection : getComponentConnections(
            workflowService.getWorkflow(workflowId))) {

            Optional<WorkflowConnectionDTO> workflowConnection = workflowConnections.stream()
                .filter(curWorkflowConnection -> Objects.equals(
                    curWorkflowConnection.workflowNodeName(), componentConnection.workflowNodeName()) &&
                    Objects.equals(curWorkflowConnection.workflowConnectionKey(), componentConnection.key()))
                .findFirst();

            if (workflowConnection.isPresent()) {
                long connectionId = workflowConnection.get()
                    .connectionId();

                if (!isConnectionUsable(connectionId, componentConnection, environment)) {
                    throw new IllegalArgumentException(
                        "Connection " + connectionId + " cannot be used for " + componentConnection.workflowNodeName() +
                            " of workflow " + workflowId + ": it must be a " + componentConnection.componentName() +
                            " connection in the " + environment + " environment");
                }

                projectDeploymentWorkflowConnections.add(
                    new ProjectDeploymentWorkflowConnection(
                        connectionId, componentConnection.key(), componentConnection.workflowNodeName()));
            } else if (componentConnection.required()) {
                throw new IllegalArgumentException(
                    "Workflow " + workflowId + " requires a " + componentConnection.componentName() +
                        " connection for " + componentConnection.workflowNodeName());
            }
        }

        return projectDeploymentWorkflowConnections;
    }

    private void validateProjectVersionPublished(Project project, int projectVersion) {
        long projectId = Objects.requireNonNull(project.getId());

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

    private void validateSameWorkspace(Project project, long mcpServerId) {
        Long projectWorkspaceId = project.getWorkspaceId();
        Long mcpServerWorkspaceId = workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServerId)
            .orElse(null);

        if (projectWorkspaceId == null || !projectWorkspaceId.equals(mcpServerWorkspaceId)) {
            throw new IllegalArgumentException(
                "Project " + project.getId() + " and MCP server " + mcpServerId + " are not in the same workspace");
        }
    }

    private void validateCallableWorkflows(List<String> workflowIds) {
        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            if (McpWorkflowUtils.getCallableTrigger(workflow) == null) {
                throw new IllegalArgumentException(
                    "Workflow " + workflowId + " has no New Workflow Call trigger and cannot be used by an MCP server");
            }
        }
    }

    private void validateProjectVersionWorkflowIds(long projectId, int projectVersion, List<String> workflowIds) {
        Set<String> projectVersionWorkflowIds = new HashSet<>(
            projectWorkflowService.getProjectWorkflowIds(projectId, projectVersion));

        List<String> foreignWorkflowIds = workflowIds.stream()
            .filter(workflowId -> !projectVersionWorkflowIds.contains(workflowId))
            .toList();

        if (!foreignWorkflowIds.isEmpty()) {
            throw new IllegalArgumentException(
                "Workflows " + foreignWorkflowIds + " do not belong to version " + projectVersion + " of project " +
                    projectId);
        }
    }

    private record WorkflowDeploymentSettings(
        List<ProjectDeploymentWorkflowConnection> connections, Map<String, Object> inputs) {
    }
}
