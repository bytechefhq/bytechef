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

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.event.ConnectionCreatedEvent;
import com.bytechef.platform.connection.event.ConnectionDeletedEvent;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnCEVersion
public class WorkspaceConnectionFacadeImpl implements WorkspaceConnectionFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceConnectionFacadeImpl.class);

    protected final ApplicationEventPublisher applicationEventPublisher;
    protected final ConnectionFacade connectionFacade;
    protected final ConnectionLifecycleFacade connectionLifecycleFacade;
    protected final ConnectionService connectionService;
    protected final ResourceVisibilityResolver resourceVisibilityResolver;
    protected final MeterRegistry meterRegistry;
    protected final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    protected final ProjectService projectService;
    protected final UserService userService;
    protected final WorkflowTestConfigurationService workflowTestConfigurationService;
    protected final WorkspaceConnectionService workspaceConnectionService;
    protected final WorkspaceFacade workspaceFacade;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI", "EI2"
    })
    public WorkspaceConnectionFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ConnectionFacade connectionFacade,
        ConnectionLifecycleFacade connectionLifecycleFacade, ConnectionService connectionService,
        ResourceVisibilityResolver resourceVisibilityResolver,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        UserService userService, WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceConnectionService workspaceConnectionService, WorkspaceFacade workspaceFacade) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.connectionFacade = connectionFacade;
        this.connectionLifecycleFacade = connectionLifecycleFacade;
        this.connectionService = connectionService;
        this.resourceVisibilityResolver = resourceVisibilityResolver;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.userService = userService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
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
    public long create(long workspaceId, ConnectionDTO connectionDTO) {
        ResourceVisibility requestedVisibility = connectionDTO.visibility();

        if (requestedVisibility != null && requestedVisibility != ResourceVisibility.PRIVATE
            && requestedVisibility != ResourceVisibility.WORKSPACE) {

            throw new ConfigurationException(
                "Only PRIVATE or WORKSPACE visibility can be set on connection creation; got %s".formatted(
                    requestedVisibility),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        // No admin gate on WORKSPACE: it is now the default every connection is created with, so requiring
        // ROLE_ADMIN for it would fail every ordinary create. The gate existed when WORKSPACE was a
        // promotion out of a private default — a meaning it no longer has.
        validateCurrentUserIsWorkspaceMember(workspaceId);

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        workspaceConnectionService.create(connectionId, workspaceId);

        // Read the persisted visibility (ConnectionFacadeImpl may have forced PRIVATE in CE/embedded)
        // so the metric tag matches what was actually stored, not the unsanitized request body.
        ConnectionDTO connection = connectionFacade.getConnection(connectionId);

        incrementCreateCounter(connection.visibility());

        // Plain CE domain event. Auditing is an EE-only concern: an EE @TransactionalEventListener
        // translates this into an audit record after commit. CE only emits the signal.
        applicationEventPublisher.publishEvent(new ConnectionCreatedEvent(connectionId, connection.visibility()));

        return connectionId;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public long registerExisting(
        long workspaceId, ConnectionDTO connectionDTO, CredentialStoreType storeType, String credentialRef) {

        Connection connection = connectionDTO.toConnection();

        connection.setType(PlatformType.AUTOMATION);

        Connection registered = connectionService.registerExisting(connection, storeType, credentialRef);

        workspaceConnectionService.create(registered.getId(), workspaceId);

        return registered.getId();
    }

    @Override
    @PreAuthorize("hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_DELETE')")
    public void delete(long connectionId) {
        // Cancel any pending OAuth refresh job before deleting any rows. If this fails, abort: a leftover
        // scheduled refresh that fires against a deleted connection wakes up the scheduler with no row to
        // refresh, which the production listener (ConnectionAfterSaveEventListener#onBeforeDelete) cannot
        // recover from once we've already torn down the workspace mapping.
        connectionLifecycleFacade.deleteScheduledConnectionRefresh(connectionId, TenantContext.getCurrentTenantId());

        workspaceConnectionService.deleteWorkspaceConnection(connectionId);

        connectionFacade.delete(connectionId);

        // Plain CE domain event; EE audits it after commit (see ConnectionCreatedEvent emission above).
        applicationEventPublisher.publishEvent(new ConnectionDeletedEvent(connectionId));
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void disconnectConnection(long connectionId) {
        // Visibility is intentionally preserved on disconnect: a workspace-shared connection that no
        // longer participates in any workflow is still discoverable by other workspace members and may
        // be re-attached to a new workflow. Demoting on disconnect would surprise admins.
        projectDeploymentWorkflowService.deleteProjectDeploymentWorkflowConnection(connectionId);
        workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_VIEW')")
    public ConnectionDTO getConnection(long connectionId) {
        return connectionFacade.getConnection(connectionId);
    }

    @Override
    @PreAuthorize("hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_EDIT')")
    public void update(long connectionId, String name, List<Tag> tags, int version) {
        connectionFacade.update(connectionId, name, tags, version);
    }

    @Override
    @PreAuthorize("hasPermission(#connectionId, 'Connection:ResourceScope', 'CONNECTION_EDIT')")
    public void updateTags(long connectionId, List<Tag> tags) {
        connectionFacade.update(connectionId, tags);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#workspaceId, 'WorkspaceScope', 'CONNECTION_VIEW')")
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        return filterVisible(
            connectionFacade.getConnections(
                componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION),
            workspaceId);
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
     * EE it delegates to the real {@code WorkspaceUserService} and blocks cross-workspace attaches. Visible to the EE
     * subclass ({@code com.bytechef.ee.automation.configuration.facade.WorkspaceConnectionFacadeImpl}) which reuses it
     * from its bulk-promote path.
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
}
