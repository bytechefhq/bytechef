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

import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_CREATED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_DELETED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_DEMOTED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_PROMOTED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_REVOKED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_SHARED;
import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_SHARES_REPLACED;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectConnection;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.dto.BulkPromoteResultDTO;
import com.bytechef.automation.configuration.dto.BulkPromoteResultDTO.BulkPromoteFailureDTO;
import com.bytechef.automation.configuration.service.ProjectConnectionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectMembershipAccessor;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.audit.AuditConnection;
import com.bytechef.platform.connection.audit.AuditConnection.AuditData;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class WorkspaceConnectionFacadeImpl implements WorkspaceConnectionFacade {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceConnectionFacadeImpl.class);

    private final ConnectionFacade connectionFacade;
    private final ConnectionLifecycleFacade connectionLifecycleFacade;
    private final ConnectionService connectionService;
    private final MeterRegistry meterRegistry;
    private final ProjectConnectionService projectConnectionService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectMembershipAccessor projectMembershipAccessor;
    private final ProjectService projectService;
    private final UserService userService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkspaceConnectionService workspaceConnectionService;
    private final WorkspaceFacade workspaceFacade;

    // Self-reference via the interface so AOP advice (audit, @Transactional propagation) fires on
    // internal re-entry (e.g. bulk-promote → promoteToWorkspace). @Lazy breaks the circular
    // dependency Spring would otherwise refuse. Tests inject via setSelf().
    @Lazy
    @Autowired
    private WorkspaceConnectionFacade self;

    /**
     * Returns the AOP-proxied self-reference. Falls back to {@code this} so unit tests that construct the impl directly
     * (without Spring) still exercise the bulk loop — note: aspect advice will NOT fire in that case, so any test that
     * needs proxy behavior must wire a real proxy via setSelf() or use Spring's test context.
     */
    private WorkspaceConnectionFacade self() {
        return self != null ? self : this;
    }

    /** Package-private for test wiring. */
    void setSelf(WorkspaceConnectionFacade self) {
        this.self = self;
    }

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI", "EI2"
    })
    public WorkspaceConnectionFacadeImpl(
        ConnectionFacade connectionFacade, ConnectionLifecycleFacade connectionLifecycleFacade,
        ConnectionService connectionService, ObjectProvider<MeterRegistry> meterRegistryProvider,
        ProjectConnectionService projectConnectionService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        ProjectMembershipAccessor projectMembershipAccessor, ProjectService projectService,
        UserService userService, WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkspaceConnectionService workspaceConnectionService, WorkspaceFacade workspaceFacade) {

        this.connectionFacade = connectionFacade;
        this.connectionLifecycleFacade = connectionLifecycleFacade;

        this.connectionService = connectionService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.projectConnectionService = projectConnectionService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectMembershipAccessor = projectMembershipAccessor;
        this.projectService = projectService;
        this.userService = userService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workspaceConnectionService = workspaceConnectionService;
        this.workspaceFacade = workspaceFacade;
    }

    private void incrementCreateCounter(ConnectionVisibility visibility) {
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
    @AuditConnection(
        event = CONNECTION_CREATED, connectionId = "#result",
        data = @AuditData(
            key = "visibility",
            // Read the PERSISTED visibility, not the request body. ConnectionFacadeImpl.create()
            // rewrites visibility to PRIVATE in CE and embedded paths; if we used
            // #connectionDTO.visibility() here the audit record would disagree with the row in the DB.
            value = "@connectionService.getConnection(#result).getVisibility().name()"))
    public long create(long workspaceId, ConnectionDTO connectionDTO) {
        ConnectionVisibility requestedVisibility = connectionDTO.visibility();

        if (requestedVisibility != null && requestedVisibility != ConnectionVisibility.PRIVATE
            && requestedVisibility != ConnectionVisibility.WORKSPACE) {

            throw new ConfigurationException(
                "Only PRIVATE or WORKSPACE visibility can be set on connection creation; got %s".formatted(
                    requestedVisibility),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        // Admin gate lives here (not @PreAuthorize) because the rule depends on the request
        // body's visibility value; the rest of the create endpoint is open to all members.
        if (requestedVisibility == ConnectionVisibility.WORKSPACE
            && !SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {

            throw new ConfigurationException(
                "Only administrators can create connections with WORKSPACE visibility",
                ConnectionErrorType.INVALID_CONNECTION);
        }

        validateCurrentUserIsWorkspaceMember(workspaceId);

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.AUTOMATION);

        workspaceConnectionService.create(connectionId, workspaceId);

        // Read the persisted visibility (ConnectionFacadeImpl may have forced PRIVATE in CE/embedded)
        // so the metric tag matches what was actually stored, not the unsanitized request body.
        ConnectionDTO connection = connectionFacade.getConnection(connectionId);

        incrementCreateCounter(connection.visibility());

        connectionLifecycleFacade.scheduleConnectionRefresh(
            connectionId, connection.parameters(), connection.authorizationType());

        return connectionId;
    }

    @Override
    @AuditConnection(event = CONNECTION_DELETED, connectionId = "#connectionId")
    public void delete(long connectionId) {
        ConnectionDTO connection = connectionFacade.getConnection(connectionId);

        // Intentionally NOT wrapped in try/finally. Previous version deleted the connection even when
        // deleteScheduledConnectionRefresh threw, leaving an orphaned scheduled refresh job that
        // targeted a now-deleted connection. Let the exception propagate with its original type so the
        // GraphQL error mapper and useFetchInterceptor can surface it accurately.
        connectionLifecycleFacade.deleteScheduledConnectionRefresh(connectionId, connection.authorizationType());

        workspaceConnectionService.deleteWorkspaceConnection(connectionId);

        connectionFacade.delete(connectionId);
    }

    @Override
    @AuditConnection(
        event = CONNECTION_DEMOTED, connectionId = "#connectionId",
        data = @AuditData(key = "toVisibility", value = "'PRIVATE'"))
    public void demoteToPrivate(long workspaceId, long connectionId) {
        // Authorize as early as possible without disclosing existence. An unauthenticated or
        // low-privilege caller must not be able to distinguish "does not exist" vs "exists but
        // forbidden" vs "exists and is in use" purely from which error message comes back, as that
        // would allow probing the (workspaceId, connectionId) namespace without being allowed to act
        // on it.
        //
        // Admins can short-circuit the creator check — their authority alone is sufficient. For
        // non-admins we must still load the row to know whether they are the creator, but we use
        // fetchConnection(...) (Optional) and conflate "not found" with "not authorized" via a
        // single INVALID_CONNECTION response so the two cases are indistinguishable from outside.
        boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);

        if (!isAdmin) {
            String currentUserLogin = SecurityUtils.getCurrentUserLogin();
            String creator;

            try {
                creator = connectionFacade.getConnection(connectionId)
                    .createdBy();
            } catch (RuntimeException loadFailure) {
                // Catches every RuntimeException (including DataAccessException, TransactionSystemException,
                // NotFoundException) — the name reflects that. Log server-side so infrastructure failures
                // surface to operators, but collapse the externally-visible error into INVALID_CONNECTION
                // so a non-admin cannot distinguish "this id does not exist" from "you are not the creator"
                // from "transient DB error" by comparing error responses.
                logger.warn(
                    "demoteToPrivate: failed to load connection id={} for authorization check", connectionId,
                    loadFailure);

                throw new ConfigurationException(
                    "Only an administrator or the connection creator may demote this connection",
                    ConnectionErrorType.INVALID_CONNECTION);
            }

            boolean isCreator = currentUserLogin != null && currentUserLogin.equals(creator);

            if (!isCreator) {
                throw new ConfigurationException(
                    "Only an administrator or the connection creator may demote this connection",
                    ConnectionErrorType.INVALID_CONNECTION);
            }
        }

        validateConnectionBelongsToWorkspace(workspaceId, connectionId);
        validateConnectionNotUsedByDeployments(connectionId);

        projectConnectionService.deleteByConnectionId(connectionId);

        connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
    }

    @Override
    public void disconnectConnection(long connectionId) {
        // Visibility is intentionally preserved on disconnect: a workspace-shared connection that no
        // longer participates in any workflow is still discoverable by other workspace members and may
        // be re-attached to a new workflow. Demoting on disconnect would surprise admins.
        projectDeploymentWorkflowService.deleteProjectDeploymentWorkflowConnection(connectionId);
        workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(connectionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId) {

        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<ConnectionDTO> allConnections = connectionFacade.getConnections(
            componentName, connectionVersion, connectionIds, tagId, environmentId, PlatformType.AUTOMATION);

        String currentUserLogin = SecurityUtils.getCurrentUserLogin();
        // Admins need visibility into every workspace member's PRIVATE connection in order to run the
        // orphan-recovery flow (demote/reassign) from the connections list; without this check an admin
        // could only act on orphans through the reassignment UI.
        boolean isAdmin = SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);

        List<Long> workspaceProjectIds = CollectionUtils.map(
            projectService.getProjects(null, null, null, null, null, workspaceId), Project::getId);

        // Narrow to projects the user is actually a member of before we build the PROJECT-visibility
        // allow-list. CE's default accessor returns the input list unchanged (no per-project membership
        // table); EE plugs in a membership-aware impl so a workspace member who is NOT a project member
        // cannot see connections shared to that project. Admins bypass this narrowing because the
        // PRIVATE branch below already grants them full visibility for orphan-recovery — keeping the
        // admin bypass consistent across visibility tiers.
        List<Long> accessibleProjectIds = isAdmin || currentUserLogin == null
            ? workspaceProjectIds
            : projectMembershipAccessor.filterByMembership(currentUserLogin, workspaceProjectIds);

        // Always call the service (even with an empty list) to keep existing test expectations stable —
        // the service short-circuits internally on an empty input.
        Set<Long> projectConnectionIds = new HashSet<>(
            CollectionUtils.map(
                projectConnectionService.getProjectConnectionsByProjectIds(accessibleProjectIds),
                ProjectConnection::getConnectionId));

        return allConnections.stream()
            .filter(connection -> switch (connection.visibility()) {
                case ORGANIZATION, WORKSPACE -> true;
                case PROJECT -> projectConnectionIds.contains(connection.id());
                // Objects.equals guards against a null currentUserLogin in service contexts where
                // SecurityUtils returns null (e.g. scheduler-initiated fetches). Admins see all PRIVATE
                // rows in the workspace so the list surface supports orphan-recovery.
                case PRIVATE -> isAdmin || Objects.equals(currentUserLogin, connection.createdBy());
            })
            .toList();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @AuditConnection(
        event = CONNECTION_PROMOTED, connectionId = "#connectionId",
        data = {
            @AuditData(key = "fromVisibility", value = "#result.name()"),
            @AuditData(key = "toVisibility", value = "'WORKSPACE'")
        })
    public ConnectionVisibility promoteToWorkspace(long workspaceId, long connectionId) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);

        ConnectionVisibility currentVisibility = connectionService.getConnection(connectionId)
            .getVisibility();

        if (currentVisibility.isAtLeast(ConnectionVisibility.WORKSPACE)) {
            throw new ConfigurationException(
                "Connection id=%s already has %s visibility and cannot be promoted to WORKSPACE".formatted(
                    connectionId, currentVisibility),
                ConnectionErrorType.CONNECTION_ALREADY_AT_TARGET_VISIBILITY);
        }

        connectionService.updateVisibility(connectionId, ConnectionVisibility.WORKSPACE);

        return currentVisibility;
    }

    @Override
    public BulkPromoteResultDTO promoteAllPrivateToWorkspace(long workspaceId) {
        // @PreAuthorize only confirms the caller is a global admin — it does NOT confirm they
        // administrate THIS workspace. Without this check a global admin could bulk-promote
        // connections in any workspace they are not a member of.
        validateCurrentUserIsWorkspaceMember(workspaceId);

        // Bulk migration helper: promote every PRIVATE connection in this workspace to WORKSPACE.
        // Single bulk fetch (avoids N+1) followed by per-row promotion. Each promotion goes through
        // promoteToWorkspace() so audit events still fire; failures are collected so the caller can
        // surface partial success instead of bailing on the first error.
        List<Long> connectionIds = CollectionUtils.map(
            workspaceConnectionService.getWorkspaceConnections(workspaceId), WorkspaceConnection::getConnectionId);

        if (connectionIds.isEmpty()) {
            return new BulkPromoteResultDTO(0, 0, 0, 0, List.of());
        }

        List<Long> privateIds = connectionService.getConnections(connectionIds)
            .stream()
            .filter(connection -> connection.getVisibility() == ConnectionVisibility.PRIVATE)
            .map(connection -> connection.getId())
            .toList();

        int promoted = 0;
        int skipped = 0;
        List<BulkPromoteFailureDTO> failures = new ArrayList<>();

        for (Long connectionId : privateIds) {
            try {
                self().promoteToWorkspace(workspaceId, connectionId);

                promoted++;
            } catch (ConfigurationException configurationException) {
                // Only CONNECTION_ALREADY_AT_TARGET_VISIBILITY is benign (the row was promoted by
                // another admin between our pre-filter read and our per-row call — a concurrent race).
                // Everything else, including INVALID_CONNECTION (cross-workspace mismatch) and
                // CONNECTION_IS_USED, is a real failure that the admin needs to see — classifying a
                // cross-workspace mismatch as "skipped" would hide an authorization bug and risks
                // enumerating connections across workspaces.
                if (configurationException.getErrorKey() == ConnectionErrorType.CONNECTION_ALREADY_AT_TARGET_VISIBILITY
                    .getErrorKey()) {

                    skipped++;

                    if (logger.isDebugEnabled()) {
                        logger.debug(
                            "Skipping promote for connection id={} — already at target visibility (race)",
                            connectionId);
                    }
                } else {
                    logger.warn(
                        "Promote failed for connection id={} in workspace={} errorKey={}: {}",
                        connectionId, workspaceId, configurationException.getErrorKey(),
                        configurationException.getMessage());

                    failures.add(BulkPromoteFailureDTO.of(
                        connectionId,
                        Integer.toString(configurationException.getErrorKey()),
                        configurationExceptionMessage(configurationException)));
                }
            } catch (RuntimeException error) {
                logger.warn(
                    "Unexpected failure promoting connection id={} in workspace={}",
                    connectionId, workspaceId, error);

                // Do NOT forward raw getMessage() for unknown exceptions — SQLException / DataAccessException
                // messages often contain JDBC URLs, bind parameters, or fully qualified table names that leak
                // schema detail into an admin toast. Sanitize to the simple class name.
                failures.add(BulkPromoteFailureDTO.of(
                    connectionId, BulkPromoteFailureDTO.UNEXPECTED_ERROR_CODE, sanitizedFailureMessage(error)));
            }
        }

        // The DTO's compact constructor enforces promoted + skipped + failed == attempted, so a loop
        // mis-account (double-counting, silently dropped row) fails loud here rather than reaching the
        // admin UI as a nonsense aggregate.
        return new BulkPromoteResultDTO(privateIds.size(), promoted, skipped, failures.size(), failures);
    }

    @Override
    @AuditConnection(
        event = CONNECTION_REVOKED, connectionId = "#connectionId",
        data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
    public void revokeConnectionFromProject(long workspaceId, long connectionId, long projectId) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);
        validateConnectionNotUsedByDeployments(connectionId);

        projectConnectionService.delete(connectionId, projectId);

        List<ProjectConnection> remaining = projectConnectionService.getConnectionProjects(connectionId);

        if (remaining.isEmpty()) {
            connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
        }
    }

    /**
     * Per-row revoke used by {@link #setConnectionProjects} — invoked through the AOP proxy so each row emits a
     * {@code CONNECTION_REVOKED} audit event under the active correlation ID. Skips the deployment-in-use guard
     * (evaluated once up-front against the net share set) and does NOT auto-demote to PRIVATE (end-state visibility is
     * resolved once after all diff steps run).
     */
    @AuditConnection(
        event = CONNECTION_REVOKED, connectionId = "#connectionId",
        data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
    public void revokeSingleProjectShareAuditOnly(long connectionId, long projectId) {
        projectConnectionService.delete(connectionId, projectId);
    }

    @Override
    @AuditConnection(
        event = CONNECTION_SHARED, connectionId = "#connectionId",
        data = @AuditData(key = "projectId", value = "T(String).valueOf(#projectId)"))
    public void shareConnectionToProject(long workspaceId, long connectionId, long projectId) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);

        ConnectionVisibility currentVisibility = connectionService.getConnection(connectionId)
            .getVisibility();

        if (currentVisibility.isAtLeast(ConnectionVisibility.WORKSPACE)) {
            throw new ConfigurationException(
                "Connection id=%s has %s visibility and cannot be shared at PROJECT level".formatted(
                    connectionId, currentVisibility),
                ConnectionErrorType.INVALID_CONNECTION);
        }

        // Idempotent by design: treat a double-share as a no-op rather than surfacing the raw
        // DuplicateKeyException from the (project_id, connection_id) unique constraint. Direct API
        // callers retrying under eventual-consistency shouldn't see a hard failure when the end state
        // already matches their intent.
        boolean alreadyShared = projectConnectionService.getConnectionProjects(connectionId)
            .stream()
            .anyMatch(projectConnection -> projectConnection.getProjectId() == projectId);

        if (!alreadyShared) {
            projectConnectionService.create(connectionId, projectId);
        }

        // Re-read inside the same @Transactional boundary to close the TOCTOU window between the
        // up-front visibility check and this persist. If admin B promoted the connection to
        // >= WORKSPACE concurrently, a blind updateVisibility(PROJECT) here would silently demote
        // the row and contradict admin B's intent. Throwing rolls back the share insert as part of
        // the same transaction, so no half-applied state remains. We classify the outcome as
        // CONNECTION_ALREADY_AT_TARGET_VISIBILITY so bulk callers (setConnectionProjects) can
        // treat it as a benign race rather than a real failure.
        ConnectionVisibility visibilityAfterShare = connectionService.getConnection(connectionId)
            .getVisibility();

        if (visibilityAfterShare.isAtLeast(ConnectionVisibility.WORKSPACE)) {
            throw new ConfigurationException(
                "Connection id=%s was concurrently promoted to %s; refusing to demote via project share"
                    .formatted(connectionId, visibilityAfterShare),
                ConnectionErrorType.CONNECTION_ALREADY_AT_TARGET_VISIBILITY);
        }

        connectionService.updateVisibility(connectionId, ConnectionVisibility.PROJECT);
    }

    @Override
    @AuditConnection(
        event = CONNECTION_SHARES_REPLACED, connectionId = "#connectionId",
        establishCorrelation = true,
        data = @AuditData(
            key = "requestedProjectIds", value = "#projectIds != null ? #projectIds.toString() : '[]'"))
    public void setConnectionProjects(long workspaceId, long connectionId, List<Long> projectIds) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);

        // Sorted sets keep audit emission order deterministic under a single correlation ID. HashSet
        // iteration order depends on JVM internals, which would cause audit consumers that expect
        // stable per-parent event sequencing to observe flaky output across restarts.
        Set<Long> requested = projectIds == null ? Set.of() : new TreeSet<>(projectIds);
        Set<Long> currentShares = new TreeSet<>(
            CollectionUtils.map(
                projectConnectionService.getConnectionProjects(connectionId), ProjectConnection::getProjectId));

        // Up-front guard against the only deployment-unsafe outcome: clearing the share list while the
        // connection is still used by a live deployment would auto-demote to PRIVATE and orphan it. For
        // any other net result (at least one share remains) the per-row guard does not apply — admins
        // must be able to reshape a share list without being blocked by deployments.
        if (requested.isEmpty() && !currentShares.isEmpty()
            && projectDeploymentWorkflowService.isConnectionUsed(connectionId)) {

            throw new ConfigurationException(
                "Connection id=%s is used by active deployments; cannot empty the project share list".formatted(
                    connectionId),
                ConnectionErrorType.CONNECTION_IS_USED);
        }

        // Share-first, revoke-last: prevents a transient PRIVATE flip that concurrent readers would
        // otherwise observe when the loop happens to process a revoke before the matching share.
        for (Long projectId : requested) {
            if (!currentShares.contains(projectId)) {
                self().shareConnectionToProject(workspaceId, connectionId, projectId);
            }
        }

        for (Long projectId : currentShares) {
            if (!requested.contains(projectId)) {
                self().revokeSingleProjectShareAuditOnly(connectionId, projectId);
            }
        }

        // End-state visibility: if the caller explicitly emptied a previously non-empty share list, demote
        // to PRIVATE. All other cases keep visibility at PROJECT (set by shareConnectionToProject) or
        // unchanged (no-op diff).
        if (!currentShares.isEmpty() && requested.isEmpty()) {
            connectionService.updateVisibility(connectionId, ConnectionVisibility.PRIVATE);
        }
    }

    /**
     * Returns the {@link ConfigurationException} message as-is. These messages are authored by the facade layer itself,
     * so they are already safe to surface to admin UIs — no sanitization needed.
     */
    private static String configurationExceptionMessage(ConfigurationException exception) {
        String message = exception.getMessage();

        return message == null || message.isBlank() ? exception.getClass()
            .getSimpleName() : message;
    }

    /**
     * Sanitized message for unknown {@link RuntimeException}s. Does NOT forward {@code getMessage()} — for
     * {@link org.springframework.dao.DataAccessException},
     * {@link org.springframework.transaction.TransactionSystemException}, or JDBC-driver exceptions this string often
     * contains SQL state, bind parameters, schema names, or connection URLs that must not leak into an admin toast.
     * Returns the simple class name so the caller gets a stable, non-sensitive classifier; the full exception is logged
     * server-side at WARN where operators can still diagnose it.
     */
    private static String sanitizedFailureMessage(Throwable error) {
        return "Unexpected error: " + error.getClass()
            .getSimpleName();
    }

    /**
     * Reject a create request whose {@code workspaceId} the caller is not a member of. In CE this is a permissive no-op
     * because {@link WorkspaceFacade#getUserWorkspaces(long)} returns every workspace (CE has no membership table). In
     * EE it delegates to the real {@code WorkspaceUserService} and blocks cross-workspace attaches.
     */
    private void validateCurrentUserIsWorkspaceMember(long workspaceId) {
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
}
