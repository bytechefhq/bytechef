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
import com.bytechef.automation.configuration.audit.WorkspaceConnectionAuditMapper;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.dto.ConnectionWorkflowDTO;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.security.ConnectionSecrets;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.authentication.NonPlatformUserAuthentication;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceConnectionFacadeImpl implements WorkspaceConnectionFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceConnectionFacadeImpl.class);

    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final String CONNECTION_TYPE = "Connection";

    protected final ConnectionFacade connectionFacade;
    protected final ConnectionLifecycleFacade connectionLifecycleFacade;
    protected final ConnectionService connectionService;
    protected final ResourceVisibilityResolver resourceVisibilityResolver;
    protected final MeterRegistry meterRegistry;
    protected final PermissionService permissionService;
    protected final ProjectDeploymentService projectDeploymentService;
    protected final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    protected final ProjectService projectService;
    protected final ProjectWorkflowService projectWorkflowService;
    protected final TagService tagService;
    protected final UserService userService;
    protected final WorkflowService workflowService;
    protected final WorkflowTestConfigurationService workflowTestConfigurationService;
    protected final List<WorkspaceConnectionContributor> workspaceConnectionContributors;
    protected final WorkspaceConnectionService workspaceConnectionService;
    protected final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI", "EI2"
    })
    public WorkspaceConnectionFacadeImpl(
        ConnectionFacade connectionFacade, ConnectionLifecycleFacade connectionLifecycleFacade,
        ConnectionService connectionService, ResourceVisibilityResolver resourceVisibilityResolver,
        ObjectProvider<MeterRegistry> meterRegistryProvider, PermissionService permissionService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, TagService tagService, UserService userService,
        WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        ObjectProvider<WorkspaceConnectionContributor> workspaceConnectionContributorProvider,
        WorkspaceConnectionService workspaceConnectionService, WorkspaceFacade workspaceFacade) {

        this.connectionFacade = connectionFacade;
        this.connectionLifecycleFacade = connectionLifecycleFacade;
        this.connectionService = connectionService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.permissionService = permissionService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.userService = userService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workspaceConnectionContributors = workspaceConnectionContributorProvider.orderedStream()
            .toList();
        this.workspaceConnectionService = workspaceConnectionService;
        this.workspaceFacade = workspaceFacade;
    }

    private void incrementCreateCounter(ResourceVisibility visibility) {
        if (meterRegistry == null) {
            return;
        }

        Counter.builder("bytechef_connection_create")
            .tag("visibility", visibility == null ? "PRIVATE" : visibility.name())
            .description("Connections created via the workspace facade, tagged by visibility")
            .register(meterRegistry)
            .increment();
    }

    @Override
    @Audited(
        event = WorkspaceConnectionAuditEvents.CONNECTION_CREATED, mapper = WorkspaceConnectionAuditMapper.class,
        data = {
            @AuditData(key = "workspaceId", value = "#workspaceId"),
            @AuditData(key = "connectionId", value = "#result"),
            @AuditData(key = "name", value = "#connectionDTO.name"),
            @AuditData(key = "componentName", value = "#connectionDTO.componentName"),
            @AuditData(key = "environmentId", value = "#connectionDTO.environmentId")
        })
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'CONNECTION_CREATE', #connectionDTO.environmentId)")
    public long create(long workspaceId, ConnectionDTO connectionDTO) {
        ResourceVisibility requestedVisibility = connectionDTO.visibility();

        if (requestedVisibility != null && requestedVisibility != ResourceVisibility.PRIVATE
            && requestedVisibility != ResourceVisibility.WORKSPACE) {

            throw new ConfigurationException(
                "Only PRIVATE or WORKSPACE visibility can be set on connection creation; got %s".formatted(
                    requestedVisibility),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        validateCurrentUserIsWorkspaceMember(workspaceId);

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        workspaceConnectionService.create(connectionId, workspaceId);

        // Read the persisted visibility (ConnectionFacadeImpl forces WORKSPACE in CE) so the metric tag matches
        // what was actually stored, not the unsanitized request body.
        ConnectionDTO connection = connectionFacade.getConnection(connectionId);

        incrementCreateCounter(connection.visibility());

        return connectionId;
    }

    @Override
    @Audited(
        event = WorkspaceConnectionAuditEvents.CONNECTION_DELETED, mapper = WorkspaceConnectionAuditMapper.class,
        data = {
            @AuditData(key = "connectionId", value = "#connectionId")
        })
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_DELETE') && " +
        "(isResourceOwner(#connectionId, 'Connection') || isTenantAdmin())")
    public void delete(long connectionId) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        for (WorkspaceConnectionContributor workspaceConnectionContributor : workspaceConnectionContributors) {
            workspaceConnectionContributor.beforeDelete(connectionId);
        }

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

        // Visibility is intentionally preserved on disconnect: a workspace-shared connection that no
        // longer participates in any workflow is still discoverable by other workspace members and may
        // be re-attached to a new workflow. Demoting on disconnect would surprise admins.
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

        return canSeeCredentials(connectionId, connectionDTO) ? connectionDTO : connectionDTO.withoutCredentials();
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironmentId(#workspaceId, 'CONNECTION_VIEW', #environmentId)")
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        List<Long> connectionIds = CollectionUtils.concatDistinct(
            CollectionUtils.map(
                workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId),
            getSharedConnectionIds());

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<ConnectionDTO> visibleConnectionDTOs = filterVisible(
            connectionFacade.getConnections(
                componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION),
            workspaceId);

        return CollectionUtils.map(
            visibleConnectionDTOs,
            connectionDTO -> canSeeCredentials(connectionDTO.id(), connectionDTO)
                ? connectionDTO : connectionDTO.withoutCredentials());
    }

    private List<Long> getSharedConnectionIds() {
        return workspaceConnectionContributors.stream()
            .flatMap(workspaceConnectionContributor -> CollectionUtils.stream(
                workspaceConnectionContributor.getSharedConnectionIds()))
            .toList();
    }

    /**
     * Narrow the loaded connections to those the current principal may see. The resolver answers in ids, so this maps
     * each DTO to the three facts it needs and filters on the returned set — keeping the resolver free of any DTO type
     * and reusable by every resource that gains visibility later.
     */
    private List<ConnectionDTO> filterVisible(List<ConnectionDTO> connectionDTOs, long workspaceId) {
        Set<Long> visibleIds = resourceVisibilityResolver.filterVisibleIds(
            "Connection", workspaceId,
            CollectionUtils.map(
                connectionDTOs,
                connectionDTO -> new VisibilityRecord(
                    connectionDTO.id(), connectionDTO.visibility(), connectionDTO.createdBy())));

        return CollectionUtils.filter(connectionDTOs, connectionDTO -> visibleIds.contains(connectionDTO.id()));
    }

    /**
     * Reject a create request whose {@code workspaceId} the caller is not a member of. In CE this is a permissive no-op
     * because {@link WorkspaceFacade#getUserWorkspaces(long)} returns every workspace (CE has no membership table). In
     * EE it delegates to the real {@code WorkspaceUserService} and blocks cross-workspace attaches.
     */
    protected void validateCurrentUserIsWorkspaceMember(long workspaceId) {
        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        if (currentUserLogin == null) {
            throw new ConfigurationException(
                "Cannot create a connection without an authenticated user", ConnectionErrorType.INVALID_CONNECTION);
        }

        User currentUser = userService.fetchUserByLogin(currentUserLogin)
            .orElseThrow(() -> new ConfigurationException(
                "Authenticated user %s was not found".formatted(currentUserLogin),
                ConnectionErrorType.INVALID_CONNECTION));

        List<Long> memberWorkspaceIds = CollectionUtils.map(
            workspaceFacade.getUserWorkspaces(currentUser.getId()), Workspace::getId);

        if (!memberWorkspaceIds.contains(workspaceId)) {
            throw new ConfigurationException(
                "Current user is not a member of workspace id=%s".formatted(workspaceId),
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKFLOW_VIEW')")
    public List<Tag> getConnectionTags(long workspaceId) {
        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<Connection> connections = connectionService.getConnections(connectionIds);

        return tagService.getTags(
            connections.stream()
                .filter(connection -> connection.getType() == PlatformType.AUTOMATION)
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
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT') && " +
        "(isResourceOwner(#connectionId, 'Connection') || isTenantAdmin())")
    public void update(long connectionId, String name, List<Tag> tags, int version) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        connectionFacade.update(connectionId, name, tags, version);
    }

    @Override
    @Audited(event = WorkspaceConnectionAuditEvents.CONNECTION_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#connectionId"),
        @AuditData(key = "name", value = "#name")
    })
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT') && " +
        "(isResourceOwner(#connectionId, 'Connection') || isTenantAdmin())")
    public void update(
        long connectionId, String name, Map<String, ?> connectionParameters, List<Tag> tags, int version) {

        requireAutomationConnection(connectionId);

        connectionFacade.update(connectionId, name, connectionParameters, tags, version);
    }

    @Override
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT') && " +
        "(isResourceOwner(#connectionId, 'Connection') || isTenantAdmin())")
    public void updateConnectionTags(long connectionId, List<Tag> tags) {
        requireAutomationConnectionUnlessConnectedUser(connectionId);

        connectionFacade.update(connectionId, tags);
    }

    @Override
    @Audited(
        event = WorkspaceConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED,
        mapper = WorkspaceConnectionAuditMapper.class, data = {
            @AuditData(key = "connectionId", value = "#connectionId")
        })
    @PreAuthorize("hasPermission(#connectionId, 'Connection', 'CONNECTION_EDIT') && " +
        "(isResourceOwner(#connectionId, 'Connection') || isTenantAdmin())")
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        requireAutomationConnection(connectionId);

        validateCredentialReplacer(connectionService.getConnection(connectionId));

        connectionFacade.updateCredentials(connectionId, parameters, version);
    }

    private boolean canSeeCredentials(long connectionId, ConnectionDTO connectionDTO) {
        if (!permissionService.hasResourceScope(connectionId, CONNECTION_TYPE, CONNECTION_EDIT)) {
            return false;
        }

        return NonPlatformUserAuthentication.isCurrent() ||
            ConnectionSecrets.canCurrentUserAccessSecrets(connectionDTO.createdBy(), connectionDTO.visibility());
    }

    private void validateCredentialReplacer(Connection connection) {
        if (!ConnectionSecrets.canCurrentUserReplaceSecrets(connection.getCreatedBy())) {
            throw new AccessDeniedException(
                "Only the connection owner or a tenant admin can replace the credentials of connection " +
                    connection.getId());
        }
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
