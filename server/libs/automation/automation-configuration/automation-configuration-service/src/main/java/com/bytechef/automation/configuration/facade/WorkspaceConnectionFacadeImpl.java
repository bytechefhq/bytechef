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

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.audit.WorkspaceConnectionAuditEvents;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.dto.ConnectionWorkflowDTO;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.security.ConnectionSecrets;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceConnectionFacadeImpl implements WorkspaceConnectionFacade {

    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final String CONNECTION_TYPE = "Connection";

    private final ConnectionFacade connectionFacade;
    private final ConnectionLifecycleFacade connectionLifecycleFacade;
    private final ConnectionService connectionService;
    private final PermissionService permissionService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkspaceConnectionService workspaceConnectionService;

    @SuppressFBWarnings("EI")
    public WorkspaceConnectionFacadeImpl(
        ConnectionFacade connectionFacade, ConnectionLifecycleFacade connectionLifecycleFacade,
        ConnectionService connectionService, PermissionService permissionService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceConnectionService workspaceConnectionService) {

        this.connectionFacade = connectionFacade;
        this.connectionLifecycleFacade = connectionLifecycleFacade;
        this.connectionService = connectionService;
        this.permissionService = permissionService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_CREATED, data = {
        @AuditData(key = "workspaceId", value = "#workspaceId"),
        @AuditData(key = "connectionId", value = "#result"),
        @AuditData(key = "name", value = "#connectionDTO.name"),
        @AuditData(key = "componentName", value = "#connectionDTO.componentName"),
        @AuditData(key = "environmentId", value = "#connectionDTO.environmentId")
    })
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'CONNECTION_CREATE', #connectionDTO.environmentId)")
    public long create(long workspaceId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        workspaceConnectionService.create(connectionId, workspaceId);

        return connectionId;
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_DELETED, data = {
        @AuditData(key = "connectionId", value = "#connectionId")
    })
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_DELETE')")
    public void delete(long connectionId) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        workspaceConnectionService.deleteWorkspaceConnection(connectionId);

        connectionFacade.delete(connectionId);
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_DISCONNECTED, data = {
        @AuditData(key = "connectionId", value = "#connectionId")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void disconnectConnection(long connectionId) {
        requireAutomationConnection(connectionId);

        projectDeploymentWorkflowService.deleteProjectDeploymentWorkflowConnection(connectionId);
        workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(connectionId);
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_DISCONNECTED, data = {
        @AuditData(key = "connectionId", value = "#connectionId"),
        @AuditData(key = "usage", value = "#usage"),
        @AuditData(key = "id", value = "#id")
    })
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void disconnectConnection(long connectionId, ConnectionWorkflowDTO.Usage usage, long id) {
        requireAutomationConnection(connectionId);

        if (usage == ConnectionWorkflowDTO.Usage.DEPLOYMENT) {
            projectDeploymentWorkflowService.deleteProjectDeploymentWorkflowConnection(id, connectionId);
        } else {
            workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(id, connectionId);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_VIEW')")
    @Transactional(readOnly = true)
    public ConnectionDTO getConnection(long connectionId) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        ConnectionDTO connectionDTO = connectionFacade.getConnection(connectionId);

        return canSeeCredentials(
            connectionDTO, permissionService.hasResourceScope(connectionId, CONNECTION_TYPE, CONNECTION_EDIT))
                ? connectionDTO : connectionDTO.withoutCredentials();
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'CONNECTION_VIEW', #environmentId)")
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
            componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION);

        Map<Integer, Boolean> canEditByEnvironmentId = new HashMap<>();
        List<ConnectionDTO> redactedConnectionDTOs = new ArrayList<>();

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            boolean canEdit = canEditByEnvironmentId.computeIfAbsent(
                connectionDTO.environmentId(),
                connectionEnvironmentId -> permissionService.hasResourceScope(
                    connectionDTO.id(), CONNECTION_TYPE, CONNECTION_EDIT));

            redactedConnectionDTOs.add(
                canSeeCredentials(connectionDTO, canEdit) ? connectionDTO : connectionDTO.withoutCredentials());
        }

        return redactedConnectionDTOs;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'CONNECTION_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<Tag> getConnectionTags(long workspaceId, Long environmentId) {
        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<Connection> connections = connectionService.getConnections(connectionIds);

        return tagService.getTags(
            connections.stream()
                .filter(connection -> connection.getType() == PlatformType.AUTOMATION)
                .filter(connection -> environmentId == null || connection.getEnvironmentId() == environmentId)
                .flatMap(connection -> CollectionUtils.stream(connection.getTagIds()))
                .distinct()
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'WORKFLOW_VIEW')")
    public List<ConnectionWorkflowDTO> getConnectionWorkflows(long connectionId) {
        requireAutomationConnection(connectionId);

        List<ConnectionWorkflowDTO> connectionWorkflowDTOs = new ArrayList<>();

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getConnectionProjectDeploymentWorkflows(connectionId);
        List<WorkflowTestConfiguration> workflowTestConfigurations =
            workflowTestConfigurationService.getConnectionWorkflowTestConfigurations(connectionId);

        List<String> workflowIds = new ArrayList<>(
            CollectionUtils.map(projectDeploymentWorkflows, ProjectDeploymentWorkflow::getWorkflowId));

        workflowIds.addAll(CollectionUtils.map(workflowTestConfigurations, WorkflowTestConfiguration::getWorkflowId));

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getWorkflowProjectWorkflows(workflowIds);

        Map<String, ProjectWorkflow> projectWorkflowMap = projectWorkflows.stream()
            .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, Function.identity(), (first, second) -> first));

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            CollectionUtils.map(projectDeploymentWorkflows, ProjectDeploymentWorkflow::getProjectDeploymentId));

        Map<Long, ProjectDeployment> projectDeploymentMap = projectDeployments.stream()
            .collect(Collectors.toMap(ProjectDeployment::getId, Function.identity()));

        List<Long> projectIds =
            new ArrayList<>(CollectionUtils.map(projectDeployments, ProjectDeployment::getProjectId));

        projectIds.addAll(CollectionUtils.map(projectWorkflows, ProjectWorkflow::getProjectId));

        Map<Long, Project> projectMap = getProjectMap(projectIds);
        Map<String, Workflow> workflowMap = getWorkflowMap(workflowIds);

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            ProjectDeployment projectDeployment =
                projectDeploymentMap.get(projectDeploymentWorkflow.getProjectDeploymentId());

            if (projectDeployment == null) {
                continue;
            }

            Project project = projectMap.get(projectDeployment.getProjectId());
            Workflow workflow = workflowMap.get(projectDeploymentWorkflow.getWorkflowId());

            if (project == null || workflow == null) {
                continue;
            }

            connectionWorkflowDTOs.add(
                toConnectionWorkflowDTO(
                    Validate.notNull(projectDeploymentWorkflow.getId(), "id"), project,
                    projectDeployment.getProjectVersion(), ConnectionWorkflowDTO.Usage.DEPLOYMENT, workflow,
                    getWorkflowUuid(projectWorkflowMap.get(projectDeploymentWorkflow.getWorkflowId()))));
        }

        for (WorkflowTestConfiguration workflowTestConfiguration : workflowTestConfigurations) {
            ProjectWorkflow projectWorkflow = projectWorkflowMap.get(workflowTestConfiguration.getWorkflowId());

            if (projectWorkflow == null) {
                continue;
            }

            Project project = projectMap.get(projectWorkflow.getProjectId());
            Workflow workflow = workflowMap.get(workflowTestConfiguration.getWorkflowId());

            if (project == null || workflow == null) {
                continue;
            }

            connectionWorkflowDTOs.add(
                toConnectionWorkflowDTO(
                    Validate.notNull(workflowTestConfiguration.getId(), "id"), project,
                    projectWorkflow.getProjectVersion(), ConnectionWorkflowDTO.Usage.EDITOR, workflow,
                    getWorkflowUuid(projectWorkflow)));
        }

        connectionWorkflowDTOs.sort(
            Comparator.comparing(ConnectionWorkflowDTO::projectName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ConnectionWorkflowDTO::workflowName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ConnectionWorkflowDTO::projectVersion, Comparator.reverseOrder())
                .thenComparing(ConnectionWorkflowDTO::usage));

        return connectionWorkflowDTOs;
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#connectionId"),
        @AuditData(key = "name", value = "#name")
    })
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')")
    public void update(long connectionId, String name, List<Tag> tags, int version) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        connectionFacade.update(connectionId, name, tags, version);
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#connectionId"),
        @AuditData(key = "name", value = "#name")
    })
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')")
    public void update(
        long connectionId, String name, Map<String, ?> connectionParameters, List<Tag> tags, int version) {

        requireAutomationConnection(connectionId);

        connectionFacade.update(connectionId, name, connectionParameters, tags, version);
    }

    @Override
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnectionTags(long connectionId, List<Tag> tags) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        connectionFacade.update(connectionId, tags);
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#connectionId")
    })
    @PreAuthorize("isTenantAdmin() or isResourceOwner(#connectionId, 'Connection')")
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        requireAutomationConnection(connectionId);

        connectionFacade.updateCredentials(connectionId, parameters, version);
    }

    private static boolean canSeeCredentials(ConnectionDTO connectionDTO, boolean canEdit) {
        if (!canEdit) {
            return false;
        }

        return NonPlatformUserAuthentication.isCurrent() ||
            ConnectionSecrets.isCurrentUserCreator(connectionDTO.createdBy());
    }

    private static @Nullable String getWorkflowUuid(@Nullable ProjectWorkflow projectWorkflow) {
        if (projectWorkflow == null) {
            return null;
        }

        return projectWorkflow.getUuidAsString();
    }

    private Map<Long, Project> getProjectMap(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }

        List<Project> projects = projectService.getProjects(List.copyOf(new LinkedHashSet<>(projectIds)));

        return projects.stream()
            .collect(Collectors.toMap(Project::getId, Function.identity()));
    }

    private Map<String, Workflow> getWorkflowMap(List<String> workflowIds) {
        if (workflowIds.isEmpty()) {
            return Map.of();
        }

        List<Workflow> workflows = workflowService.getWorkflows(List.copyOf(new LinkedHashSet<>(workflowIds)));

        return workflows.stream()
            .collect(Collectors.toMap(Workflow::getId, Function.identity()));
    }

    private static ConnectionWorkflowDTO toConnectionWorkflowDTO(
        long id, Project project, int projectVersion, ConnectionWorkflowDTO.Usage usage, Workflow workflow,
        @Nullable String workflowUuid) {

        return new ConnectionWorkflowDTO(
            id, Validate.notNull(project.getId(), "projectId"), project.getName(), projectVersion, usage,
            workflow.getId(), workflow.getLabel(), workflowUuid);
    }

    private void requireAutomationConnection(long connectionId) {
        Connection connection = connectionService.getConnection(connectionId);

        if (connection.getType() != PlatformType.AUTOMATION) {
            throw new NoSuchElementException("Connection does not exist for id=" + connectionId);
        }
    }

    private void requireAutomationConnectionUnlessConnectedUser(long connectionId) {
        if (NonPlatformUserAuthentication.isCurrent()) {
            return;
        }

        requireAutomationConnection(connectionId);
    }
}
