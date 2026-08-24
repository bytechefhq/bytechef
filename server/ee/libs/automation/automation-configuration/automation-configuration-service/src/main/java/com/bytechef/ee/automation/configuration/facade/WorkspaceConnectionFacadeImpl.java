/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_ACCESS_GRANTED;
import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_ACCESS_REVOKED;
import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_CREDENTIALS_UPDATED;
import static com.bytechef.ee.platform.connection.audit.ConnectionAuditEvent.CONNECTION_VISIBILITY_CHANGED;

import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.ee.platform.connection.audit.AuditConnection;
import com.bytechef.ee.platform.connection.audit.AuditConnection.AuditData;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tag.service.TagService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EE implementation of the workspace connection visibility and sharing operations. Extends the CE CRUD impl so the EE
 * bean satisfies both the CE base interface (REST/GraphQL CRUD consumers) and the EE sub-interface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("NM")
public class WorkspaceConnectionFacadeImpl
    extends com.bytechef.automation.configuration.facade.WorkspaceConnectionFacadeImpl
    implements WorkspaceConnectionFacade {

    private static final String CONNECTION = "Connection";

    private final ResourceGrantService resourceGrantService;
    private final ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI", "EI2"
    })
    public WorkspaceConnectionFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ConnectionFacade connectionFacade,
        ConnectionLifecycleFacade connectionLifecycleFacade, ConnectionService connectionService,
        ResourceVisibilityResolver resourceVisibilityResolver,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ResourceGrantService resourceGrantService,
        ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry, TagService tagService,
        UserService userService, WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceConnectionService workspaceConnectionService, WorkspaceFacade workspaceFacade,
        WorkspaceUserService workspaceUserService) {

        super(
            applicationEventPublisher, connectionFacade, connectionLifecycleFacade, connectionService,
            resourceVisibilityResolver, meterRegistryProvider, projectDeploymentWorkflowService, projectService,
            tagService, userService, workflowTestConfigurationService, workspaceConnectionService, workspaceFacade);

        this.resourceGrantService = resourceGrantService;
        this.resourceVisibilityPolicyRegistry = resourceVisibilityPolicyRegistry;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @AuditConnection(
        event = CONNECTION_VISIBILITY_CHANGED, connectionId = "#connectionId",
        data = @AuditData(key = "toVisibility", value = "#visibility.name()"))
    @PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
        "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
    public void setConnectionVisibility(long workspaceId, long connectionId, ResourceVisibility visibility) {
        if (!resourceVisibilityPolicyRegistry.supports(CONNECTION, visibility)) {
            throw new ConfigurationException(
                "Connection does not support %s visibility".formatted(visibility),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        if (visibility == ResourceVisibility.ORGANIZATION) {
            throw new ConfigurationException(
                "ORGANIZATION visibility is set through the organization connection facade, not the workspace facade",
                ConnectionErrorType.INVALID_CONNECTION);
        }

        validateConnectionBelongsToWorkspace(workspaceId, connectionId);

        // Only narrowing can strand a deployment, so widening does not pay for the query.
        if (visibility == ResourceVisibility.PRIVATE) {
            validateConnectionNotUsedByDeployments(connectionId);
        }

        connectionService.updateVisibility(connectionId, visibility);
    }

    @Override
    @AuditConnection(
        event = CONNECTION_ACCESS_GRANTED, connectionId = "#connectionId",
        data = @AuditData(key = "targetUserId", value = "#userId"))
    @PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
        "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
    public void grantConnectionAccess(long workspaceId, long connectionId, long userId) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);
        validateGranteeIsWorkspaceMember(workspaceId, userId);

        resourceGrantService.grant(CONNECTION, connectionId, userId);
    }

    @Override
    @AuditConnection(
        event = CONNECTION_ACCESS_REVOKED, connectionId = "#connectionId",
        data = @AuditData(key = "targetUserId", value = "#userId"))
    @PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
        "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
    public void revokeConnectionAccess(long workspaceId, long connectionId, long userId) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);

        // No membership check on the way out: someone removed from the workspace must still be revocable, and
        // revoking a grant that is not there is already a no-op.
        resourceGrantService.revoke(CONNECTION, connectionId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@permissionService.isResourceOwner('Connection', #connectionId) || " +
        "@permissionService.hasResourceRole(#connectionId, 'Connection', 'ADMIN')")
    public List<Long> getConnectionGrants(long workspaceId, long connectionId) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);

        return resourceGrantService.getGrantedUserIds(CONNECTION, connectionId);
    }

    @Override
    public void delete(long connectionId) {
        // Grants first: resource_grant.resource_id is polymorphic and carries no foreign key, so a grant left
        // behind would attach to whatever later recycles this id.
        resourceGrantService.deleteGrants(CONNECTION, connectionId);

        super.delete(connectionId);
    }

    /**
     * Overridden purely to attach the audit event; the guard and the behaviour are inherited. No payload beyond
     * {@code connectionId} — no credential material is ever recorded.
     */
    @Override
    @AuditConnection(event = CONNECTION_CREDENTIALS_UPDATED, connectionId = "#connectionId")
    public void updateConnectionCredentials(long connectionId, Map<String, ?> parameters, int version) {
        super.updateConnectionCredentials(connectionId, parameters, version);
    }

    private void validateConnectionBelongsToWorkspace(long workspaceId, long connectionId) {
        List<Long> workspaceConnectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (!workspaceConnectionIds.contains(connectionId)) {
            throw new ConfigurationException(
                "Connection id=%s does not belong to workspace id=%s".formatted(connectionId, workspaceId),
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }

    private void validateConnectionNotUsedByDeployments(long connectionId) {
        if (projectDeploymentWorkflowService.isConnectionUsed(connectionId)) {
            throw new ConfigurationException(
                "Connection id=%s is used by active deployments".formatted(connectionId),
                ConnectionErrorType.CONNECTION_IS_USED);
        }
    }

    private void validateGranteeIsWorkspaceMember(long workspaceId, long userId) {
        if (workspaceUserService.fetchWorkspaceUser(userId, workspaceId)
            .isEmpty()) {

            // Deliberately the same error as an unknown connection: a grantor must not be able to enumerate user
            // ids by distinguishing "no such user" from "not a member of this workspace".
            throw new ConfigurationException(
                "User id=%s is not a member of workspace id=%s".formatted(userId, workspaceId),
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }
}
