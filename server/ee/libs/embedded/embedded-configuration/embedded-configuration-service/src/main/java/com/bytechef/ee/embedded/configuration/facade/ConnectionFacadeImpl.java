/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.audit.ConnectionAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationConnectionWorkflowDTO;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.security.ConnectionOwnershipExemption;
import com.bytechef.platform.connection.security.ConnectionSecrets;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service("com.bytechef.ee.embedded.configuration.facade.ConnectionFacade")
@ConditionalOnEEVersion
public class ConnectionFacadeImpl implements ConnectionFacade {

    private static final String CONNECTION_EDIT = "CONNECTION_EDIT";
    private static final String CONNECTION_TYPE = "Connection";
    private static final String CONNECTION_VIEW = "CONNECTION_VIEW";

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserService connectedUserService;
    private final com.bytechef.platform.connection.facade.ConnectionFacade connectionFacade;
    private final ConnectionOwnershipExemption connectionOwnershipExemption;
    private final ConnectionService connectionService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final PermissionService permissionService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public ConnectionFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionService connectedUserConnectionService,
        ConnectedUserProjectService connectedUserProjectService, ConnectedUserService connectedUserService,
        com.bytechef.platform.connection.facade.ConnectionFacade connectionFacade,
        ConnectionOwnershipExemption connectionOwnershipExemption, ConnectionService connectionService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationService integrationService, IntegrationWorkflowService integrationWorkflowService,
        @Lazy PermissionService permissionService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectWorkflowService projectWorkflowService, TagService tagService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
        this.connectionOwnershipExemption = connectionOwnershipExemption;
        this.connectionService = connectionService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.permissionService = permissionService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_CREATED, data = {
        @AuditData(key = "connectionId", value = "#result"),
        @AuditData(key = "name", value = "#connectionDTO.name"),
        @AuditData(key = "componentName", value = "#connectionDTO.componentName"),
        @AuditData(key = "shared", value = "#shared")
    })
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('CONNECTION_CREATE', #connectionDTO.environmentId)")
    public long createConnection(ConnectionDTO connectionDTO, boolean shared) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        if (shared) {
            connectedUserConnectionService.updateShared(connectionId, true);
        }

        return connectionId;
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_DELETED, data = {
        @AuditData(key = "connectionId", value = "#id")
    })
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_DELETE')")
    public void deleteConnection(long id) {
        requireAdminConnection(id);

        connectionFacade.delete(id, () -> connectedUserConnectionService.deleteByConnectionId(id));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_VIEW')")
    public ConnectionDTO getConnection(long id) {
        requireAdminConnection(id);

        ConnectionDTO connectionDTO = connectionFacade.getConnection(id);

        return canSeeCredentials(connectionDTO,
            permissionService.hasResourceScope(id, CONNECTION_TYPE, CONNECTION_EDIT))
                ? connectionDTO : connectionDTO.withoutCredentials();
    }

    @Override
    @PreAuthorize("hasEmbeddedScopeInEnvironmentId('CONNECTION_VIEW', #environmentId)")
    public List<ConnectionDTO> getConnections(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Long environmentId,
        @Nullable Long tagId) {

        List<ConnectionDTO> connectionDTOs = connectionFacade.getConnections(
            componentName, connectionVersion, List.of(), tagId, environmentId, PlatformType.EMBEDDED);

        Set<Long> connectionIds = connectionDTOs.stream()
            .map(ConnectionDTO::id)
            .collect(Collectors.toSet());

        Set<Long> connectedUserOwnedConnectionIds =
            connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(connectionIds);

        Map<Integer, Boolean> canEditByEnvironmentId = new HashMap<>();
        List<ConnectionDTO> redactedConnectionDTOs = new ArrayList<>();

        for (ConnectionDTO connectionDTO : connectionDTOs) {
            if (connectedUserOwnedConnectionIds.contains(connectionDTO.id())) {
                continue;
            }

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
    @PreAuthorize("hasEmbeddedScope('CONNECTION_VIEW')")
    public List<Tag> getConnectionTags() {
        Set<Integer> viewableEnvironmentIds = Arrays.stream(Environment.values())
            .filter(environment -> permissionService.hasEmbeddedScope(CONNECTION_VIEW, environment))
            .map(Environment::ordinal)
            .collect(Collectors.toSet());

        List<Connection> connections = connectionService.getConnections(PlatformType.EMBEDDED)
            .stream()
            .filter(connection -> viewableEnvironmentIds.contains(connection.getEnvironmentId()))
            .toList();

        Set<Long> connectionIds = connections.stream()
            .map(Connection::getId)
            .collect(Collectors.toSet());

        Set<Long> connectedUserOwnedConnectionIds =
            connectedUserConnectionMembership.getConnectedUserOwnedConnectionIds(connectionIds);

        List<Long> tagIds = connections.stream()
            .filter(connection -> !connectedUserOwnedConnectionIds.contains(connection.getId()))
            .flatMap(connection -> CollectionUtils.stream(connection.getTagIds()))
            .distinct()
            .toList();

        return tagService.getTags(tagIds);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_VIEW')")
    @Transactional(readOnly = true)
    public List<IntegrationConnectionWorkflowDTO> getConnectionWorkflows(long id) {
        requireAdminConnection(id);

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getConnectionIntegrationInstanceConfigurationWorkflows(id);
        List<WorkflowTestConfiguration> workflowTestConfigurations =
            workflowTestConfigurationService.getConnectionWorkflowTestConfigurations(id);
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows =
            projectDeploymentWorkflowService.getConnectionProjectDeploymentWorkflows(id);

        List<String> integrationCandidateWorkflowIds = new ArrayList<>(
            CollectionUtils.map(
                integrationInstanceConfigurationWorkflows, IntegrationInstanceConfigurationWorkflow::getWorkflowId));

        integrationCandidateWorkflowIds.addAll(
            CollectionUtils.map(workflowTestConfigurations, WorkflowTestConfiguration::getWorkflowId));

        Map<String, IntegrationWorkflow> integrationWorkflowMap = getIntegrationWorkflowMap(
            integrationCandidateWorkflowIds);
        Map<Long, IntegrationInstanceConfiguration> integrationInstanceConfigurationMap =
            getIntegrationInstanceConfigurationMap(
                CollectionUtils.map(
                    integrationInstanceConfigurationWorkflows,
                    IntegrationInstanceConfigurationWorkflow::getIntegrationInstanceConfigurationId));

        List<IntegrationRow> integrationRows = new ArrayList<>();
        List<ConnectedUserRow> connectedUserRows = new ArrayList<>();

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {
            IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationMap.get(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

            if (integrationInstanceConfiguration == null) {
                continue;
            }

            IntegrationWorkflow integrationWorkflow = integrationWorkflowMap.get(
                integrationInstanceConfigurationWorkflow.getWorkflowId());

            integrationRows.add(
                new IntegrationRow(
                    Validate.notNull(integrationInstanceConfigurationWorkflow.getId(), "id"),
                    integrationInstanceConfiguration.getIntegrationId(),
                    integrationInstanceConfiguration.getIntegrationVersion(),
                    IntegrationConnectionWorkflowDTO.Usage.DEPLOYMENT,
                    integrationInstanceConfigurationWorkflow.getWorkflowId(),
                    integrationWorkflow == null ? null : integrationWorkflow.getUuidAsString()));
        }

        for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflows) {
            connectedUserRows.add(
                new ConnectedUserRow(
                    Validate.notNull(projectDeploymentWorkflow.getId(), "id"),
                    IntegrationConnectionWorkflowDTO.Usage.DEPLOYMENT, projectDeploymentWorkflow.getWorkflowId()));
        }

        for (WorkflowTestConfiguration workflowTestConfiguration : workflowTestConfigurations) {
            long workflowTestConfigurationId = Validate.notNull(workflowTestConfiguration.getId(), "id");

            IntegrationWorkflow integrationWorkflow = integrationWorkflowMap.get(
                workflowTestConfiguration.getWorkflowId());

            if (integrationWorkflow == null) {
                connectedUserRows.add(
                    new ConnectedUserRow(
                        workflowTestConfigurationId, IntegrationConnectionWorkflowDTO.Usage.EDITOR,
                        workflowTestConfiguration.getWorkflowId()));

                continue;
            }

            integrationRows.add(
                new IntegrationRow(
                    workflowTestConfigurationId, integrationWorkflow.getIntegrationId(),
                    integrationWorkflow.getIntegrationVersion(), IntegrationConnectionWorkflowDTO.Usage.EDITOR,
                    workflowTestConfiguration.getWorkflowId(), integrationWorkflow.getUuidAsString()));
        }

        List<String> workflowIds = new ArrayList<>(CollectionUtils.map(integrationRows, IntegrationRow::workflowId));

        workflowIds.addAll(CollectionUtils.map(connectedUserRows, ConnectedUserRow::workflowId));

        Map<String, Workflow> workflowMap = getWorkflowMap(workflowIds);

        List<IntegrationConnectionWorkflowDTO> integrationConnectionWorkflowDTOs = new ArrayList<>(
            toIntegrationConnectionWorkflowDTOs(integrationRows, workflowMap));

        integrationConnectionWorkflowDTOs.addAll(toConnectedUserConnectionWorkflowDTOs(connectedUserRows, workflowMap));

        integrationConnectionWorkflowDTOs.sort(
            Comparator.comparing(IntegrationConnectionWorkflowDTO::parentType)
                .thenComparing(IntegrationConnectionWorkflowDTO::parentName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(IntegrationConnectionWorkflowDTO::workflowName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(IntegrationConnectionWorkflowDTO::parentVersion, Comparator.reverseOrder())
                .thenComparing(IntegrationConnectionWorkflowDTO::usage));

        return integrationConnectionWorkflowDTOs;
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('CONNECTION_VIEW')")
    public Set<Long> getSharedConnectionIds() {
        return connectedUserConnectionService.getSharedConnectionIds();
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#id"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "shared", value = "#shared")
    })
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnection(long id, String name, List<Tag> tags, @Nullable Boolean shared, int version) {
        requireAdminConnection(id);

        connectionFacade.update(id, name, tags, version);

        if (shared != null) {
            connectedUserConnectionService.updateShared(id, shared);
        }
    }

    @Override
    @Transactional
    @Audited(event = ConnectionAuditEvents.CONNECTION_CREDENTIALS_UPDATED, data = {
        @AuditData(key = "connectionId", value = "#id")
    })
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnectionCredentials(long id, Map<String, ?> parameters, int version) {
        requireAdminConnection(id);

        connectionFacade.updateCredentials(id, parameters, version);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#id, 'Connection', 'CONNECTION_EDIT')")
    public void updateConnectionTags(long id, List<Tag> tags) {
        requireAdminConnection(id);

        connectionFacade.update(id, tags);
    }

    private boolean canSeeCredentials(ConnectionDTO connectionDTO, boolean canEdit) {
        return canEdit && ConnectionSecrets.isCurrentUserCreator(connectionDTO.createdBy()) &&
            !connectionOwnershipExemption.isConnectedUserOwned(connectionDTO.id());
    }

    private Map<Long, ConnectedUserProject> getConnectedUserProjectMap(List<Long> projectIds) {
        Map<Long, ConnectedUserProject> connectedUserProjectMap = new HashMap<>();

        for (long projectId : new LinkedHashSet<>(projectIds)) {
            Optional<ConnectedUserProject> connectedUserProjectOptional =
                connectedUserProjectService.fetchProjectConnectedUserProject(projectId);

            connectedUserProjectOptional.ifPresent(
                connectedUserProject -> connectedUserProjectMap.put(projectId, connectedUserProject));
        }

        return connectedUserProjectMap;
    }

    private Map<Long, ConnectedUser> getConnectedUserMap(List<Long> connectedUserIds) {
        if (connectedUserIds.isEmpty()) {
            return Map.of();
        }

        List<ConnectedUser> connectedUsers = connectedUserService.getConnectedUsers(
            List.copyOf(new LinkedHashSet<>(connectedUserIds)));

        return connectedUsers.stream()
            .collect(Collectors.toMap(ConnectedUser::getId, Function.identity()));
    }

    private static String getConnectedUserName(ConnectedUser connectedUser) {
        String name = connectedUser.getName();

        if (name != null && !name.isBlank()) {
            return name;
        }

        String email = connectedUser.getEmail();

        if (email != null && !email.isBlank()) {
            return email;
        }

        return connectedUser.getExternalId();
    }

    private Map<Long, IntegrationInstanceConfiguration> getIntegrationInstanceConfigurationMap(
        List<Long> integrationInstanceConfigurationIds) {

        if (integrationInstanceConfigurationIds.isEmpty()) {
            return Map.of();
        }

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                List.copyOf(new LinkedHashSet<>(integrationInstanceConfigurationIds)));

        return integrationInstanceConfigurations.stream()
            .collect(Collectors.toMap(IntegrationInstanceConfiguration::getId, Function.identity()));
    }

    private Map<Long, Integration> getIntegrationMap(List<Long> integrationIds) {
        if (integrationIds.isEmpty()) {
            return Map.of();
        }

        List<Integration> integrations = integrationService.getIntegrations(
            List.copyOf(new LinkedHashSet<>(integrationIds)));

        return integrations.stream()
            .collect(Collectors.toMap(Integration::getId, Function.identity()));
    }

    private Map<String, IntegrationWorkflow> getIntegrationWorkflowMap(List<String> workflowIds) {
        Map<String, IntegrationWorkflow> integrationWorkflowMap = new HashMap<>();

        for (String workflowId : new LinkedHashSet<>(workflowIds)) {
            Optional<IntegrationWorkflow> integrationWorkflowOptional =
                integrationWorkflowService.fetchWorkflowIntegrationWorkflow(workflowId);

            integrationWorkflowOptional.ifPresent(
                integrationWorkflow -> integrationWorkflowMap.put(workflowId, integrationWorkflow));
        }

        return integrationWorkflowMap;
    }

    private Map<String, Workflow> getWorkflowMap(List<String> workflowIds) {
        if (workflowIds.isEmpty()) {
            return Map.of();
        }

        List<Workflow> workflows = workflowService.getWorkflows(List.copyOf(new LinkedHashSet<>(workflowIds)));

        return workflows.stream()
            .collect(Collectors.toMap(Workflow::getId, Function.identity()));
    }

    private List<IntegrationConnectionWorkflowDTO> toConnectedUserConnectionWorkflowDTOs(
        List<ConnectedUserRow> connectedUserRows, Map<String, Workflow> workflowMap) {

        if (connectedUserRows.isEmpty()) {
            return List.of();
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getWorkflowProjectWorkflows(
            List.copyOf(new LinkedHashSet<>(CollectionUtils.map(connectedUserRows, ConnectedUserRow::workflowId))));

        Map<String, ProjectWorkflow> projectWorkflowMap = projectWorkflows.stream()
            .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, Function.identity(), (first, second) -> first));
        Map<Long, ConnectedUserProject> connectedUserProjectMap = getConnectedUserProjectMap(
            CollectionUtils.map(projectWorkflows, ProjectWorkflow::getProjectId));
        Map<Long, ConnectedUser> connectedUserMap = getConnectedUserMap(
            CollectionUtils.map(
                List.copyOf(connectedUserProjectMap.values()), ConnectedUserProject::getConnectedUserId));

        List<IntegrationConnectionWorkflowDTO> connectedUserConnectionWorkflowDTOs = new ArrayList<>();

        for (ConnectedUserRow connectedUserRow : connectedUserRows) {
            ProjectWorkflow projectWorkflow = projectWorkflowMap.get(connectedUserRow.workflowId());

            if (projectWorkflow == null) {
                continue;
            }

            ConnectedUserProject connectedUserProject = connectedUserProjectMap.get(projectWorkflow.getProjectId());

            if (connectedUserProject == null) {
                continue;
            }

            ConnectedUser connectedUser = connectedUserMap.get(connectedUserProject.getConnectedUserId());
            Workflow workflow = workflowMap.get(connectedUserRow.workflowId());

            if (connectedUser == null || workflow == null) {
                continue;
            }

            connectedUserConnectionWorkflowDTOs.add(
                new IntegrationConnectionWorkflowDTO(
                    connectedUserRow.id(), Validate.notNull(connectedUser.getId(), "id"),
                    getConnectedUserName(connectedUser), IntegrationConnectionWorkflowDTO.ParentType.CONNECTED_USER,
                    projectWorkflow.getProjectVersion(), connectedUserRow.usage(), connectedUserRow.workflowId(),
                    workflow.getLabel(), projectWorkflow.getUuidAsString()));
        }

        return connectedUserConnectionWorkflowDTOs;
    }

    private List<IntegrationConnectionWorkflowDTO> toIntegrationConnectionWorkflowDTOs(
        List<IntegrationRow> integrationRows, Map<String, Workflow> workflowMap) {

        Map<Long, Integration> integrationMap = getIntegrationMap(
            CollectionUtils.map(integrationRows, IntegrationRow::integrationId));

        List<IntegrationConnectionWorkflowDTO> integrationConnectionWorkflowDTOs = new ArrayList<>();

        for (IntegrationRow integrationRow : integrationRows) {
            Integration integration = integrationMap.get(integrationRow.integrationId());
            Workflow workflow = workflowMap.get(integrationRow.workflowId());

            if (integration == null || workflow == null) {
                continue;
            }

            integrationConnectionWorkflowDTOs.add(
                new IntegrationConnectionWorkflowDTO(
                    integrationRow.id(), integrationRow.integrationId(), integration.getName(),
                    IntegrationConnectionWorkflowDTO.ParentType.INTEGRATION, integrationRow.integrationVersion(),
                    integrationRow.usage(), integrationRow.workflowId(), workflow.getLabel(),
                    integrationRow.workflowUuid()));
        }

        return integrationConnectionWorkflowDTOs;
    }

    private void requireAdminConnection(long id) {
        boolean embedded = connectionService.fetchConnection(id)
            .map(Connection::getType)
            .filter(type -> type == PlatformType.EMBEDDED)
            .isPresent();

        if (!embedded || connectedUserConnectionMembership.isOwnedByConnectedUser(id)) {
            throw new NoSuchElementException("Connection id=%s not found".formatted(id));
        }
    }

    private record ConnectedUserRow(long id, IntegrationConnectionWorkflowDTO.Usage usage, String workflowId) {
    }

    private record IntegrationRow(
        long id, long integrationId, int integrationVersion, IntegrationConnectionWorkflowDTO.Usage usage,
        String workflowId, @Nullable String workflowUuid) {
    }
}
