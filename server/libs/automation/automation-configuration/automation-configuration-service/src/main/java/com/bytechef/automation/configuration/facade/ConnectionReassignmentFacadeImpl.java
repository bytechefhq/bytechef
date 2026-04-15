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

import static com.bytechef.platform.connection.audit.ConnectionAuditEvent.CONNECTION_REASSIGNED;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.automation.configuration.dto.BulkReassignResultDTO.BulkReassignFailureDTO;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.audit.AuditConnection;
import com.bytechef.platform.connection.audit.AuditConnection.AuditData;
import com.bytechef.platform.connection.audit.AuditCorrelation;
import com.bytechef.platform.connection.audit.ConnectionAuditPublisher;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectionReassignmentFacadeImpl implements ConnectionReassignmentFacade {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionReassignmentFacadeImpl.class);

    private final ConnectionAuditPublisher connectionAuditPublisher;
    private final ConnectionService connectionService;
    private final MeterRegistry meterRegistry;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final WorkspaceConnectionService workspaceConnectionService;

    // Self-reference via the interface so AOP advice (@Transactional propagation for the per-row
    // REQUIRES_NEW worker) fires on internal re-entry (bulk mark-pending → markSingleConnectionPendingReassignment).
    // @Lazy breaks the circular dependency Spring would otherwise refuse. Tests inject via setSelf().
    @Lazy
    @Autowired
    private ConnectionReassignmentFacade self;

    /**
     * Returns the AOP-proxied self-reference. Falls back to {@code this} so unit tests that construct the impl directly
     * (without Spring) still exercise the bulk loop — note: proxy-driven tx propagation will NOT fire in that case, so
     * any test that needs the per-row {@code REQUIRES_NEW} boundary must wire a real proxy via setSelf() or use
     * Spring's test context.
     */
    private ConnectionReassignmentFacade self() {
        return self != null ? self : this;
    }

    /** Package-private for test wiring. */
    void setSelf(ConnectionReassignmentFacade self) {
        this.self = self;
    }

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI", "EI2"
    })
    public ConnectionReassignmentFacadeImpl(
        ConnectionAuditPublisher connectionAuditPublisher, ConnectionService connectionService,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, UserService userService,
        WorkflowService workflowService, WorkspaceConnectionService workspaceConnectionService) {

        this.connectionAuditPublisher = connectionAuditPublisher;
        this.connectionService = connectionService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.userService = userService;
        this.workflowService = workflowService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    private void recordAuditFailure() {
        if (meterRegistry != null) {
            Counter.builder("bytechef_connection_audit_failed")
                .register(meterRegistry)
                .increment();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionReassignmentItem> getUnresolvedConnections(long workspaceId, String userLogin) {
        List<Long> connectionIds = workspaceConnectionService.getWorkspaceConnections(workspaceId)
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .toList();

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        List<Connection> userConnections = connectionService.getConnections(connectionIds)
            .stream()
            .filter(connection -> userLogin.equals(connection.getCreatedBy()))
            .toList();

        Set<Long> userConnectionIds = userConnections.stream()
            .map(Connection::getId)
            .collect(Collectors.toSet());

        List<ProjectDeploymentWorkflow> allDeploymentWorkflows =
            projectDeploymentWorkflowService
                .getProjectDeploymentWorkflowsByConnectionIds(new ArrayList<>(userConnectionIds));

        Map<Long, Long> workflowCountByConnectionId = new HashMap<>();

        for (ProjectDeploymentWorkflow deploymentWorkflow : allDeploymentWorkflows) {
            for (ProjectDeploymentWorkflowConnection workflowConnection : deploymentWorkflow.getConnections()) {
                long connectionId = workflowConnection.getConnectionId();

                if (userConnectionIds.contains(connectionId)) {
                    workflowCountByConnectionId.merge(connectionId, 1L, Long::sum);
                }
            }
        }

        return userConnections.stream()
            .map(connection -> new ConnectionReassignmentItem(
                connection.getId(),
                connection.getName(),
                connection.getVisibility(),
                connection.getEnvironmentId(),
                workflowCountByConnectionId.getOrDefault(connection.getId(), 0L)
                    .intValue()))
            .toList();
    }

    @Override
    public BulkReassignResultDTO markConnectionsPendingReassignment(long workspaceId, String userLogin) {
        List<ConnectionReassignmentItem> connections = getUnresolvedConnections(workspaceId, userLogin);

        int updated = 0;
        int skipped = 0;
        List<BulkReassignFailureDTO> failures = new ArrayList<>();

        // Per-row work is dispatched through self().markSingleConnectionPendingReassignment so each row
        // runs in its own REQUIRES_NEW transaction. Without the proxy boundary, the class-level
        // @Transactional(REQUIRED) would combine every row into one transaction: a single failing
        // updateConnectionStatus would mark the outer tx rollback-only and wipe every previously
        // "updated" row at commit time — poisoning the partial-failure contract this DTO advertises.
        for (ConnectionReassignmentItem item : connections) {
            try {
                MarkPendingOutcome outcome = self().markSingleConnectionPendingReassignment(item.connectionId());

                if (outcome == MarkPendingOutcome.UPDATED) {
                    updated++;
                } else {
                    skipped++;
                }
            } catch (ConfigurationException configurationException) {
                String message = configurationException.getMessage() != null
                    ? configurationException.getMessage()
                    : configurationException.getClass()
                        .getSimpleName();

                failures.add(BulkReassignFailureDTO.of(
                    item.connectionId(), String.valueOf(configurationException.getErrorKey()), message));

                logger.warn(
                    "Failed to mark connection id={} as PENDING_REASSIGNMENT for workspace={} user={}; continuing",
                    item.connectionId(), workspaceId, userLogin, configurationException);
            } catch (RuntimeException exception) {
                // Sanitize unknown exceptions — never forward raw JDBC / SQL detail into an admin toast.
                failures.add(BulkReassignFailureDTO.of(
                    item.connectionId(), BulkReassignFailureDTO.UNEXPECTED_ERROR_CODE,
                    "Unexpected error: " + exception.getClass()
                        .getSimpleName()));

                logger.error(
                    "Unexpected failure marking connection id={} as PENDING_REASSIGNMENT for workspace={} user={};"
                        + " continuing",
                    item.connectionId(), workspaceId, userLogin, exception);
            }
        }

        int failed = failures.size();

        if (failed > 0) {
            logger.error(
                "markConnectionsPendingReassignment completed with {} failure(s) out of {} for workspace={} user={};"
                    + " operators should reconcile manually",
                failed, connections.size(), workspaceId, userLogin);
        }

        return new BulkReassignResultDTO(connections.size(), updated, skipped, failed, failures);
    }

    /**
     * Per-row REQUIRES_NEW worker for {@link #markConnectionsPendingReassignment}. The full read-check-write triple is
     * inside this transaction boundary: the status read, the {@code canTransitionTo} guard, and the update all happen
     * under the same fresh transaction, so any throw rolls back only this row's work and the enclosing batch tx is
     * untouched.
     *
     * <p>
     * A row already in a terminal state (e.g. {@code REVOKED}) returns {@link MarkPendingOutcome#SKIPPED} rather than
     * throwing — that's a benign outcome that the batch counts as {@code skipped}, distinct from real failures.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MarkPendingOutcome markSingleConnectionPendingReassignment(long connectionId) {
        // Re-read current status inside the REQUIRES_NEW tx so we close the TOCTOU window with
        // getUnresolvedConnections: another admin may have revoked the row between the enumeration
        // and our per-row call. Rehydrated from a primitive INT ordinal by Connection.getStatus, so
        // no null-guard.
        ConnectionStatus currentStatus = connectionService.getConnection(connectionId)
            .getStatus();

        if (!currentStatus.canTransitionTo(ConnectionStatus.PENDING_REASSIGNMENT)) {
            if (logger.isInfoEnabled()) {
                logger.info(
                    "Skipping connection id={}: status={} cannot transition to PENDING_REASSIGNMENT",
                    connectionId, currentStatus);
            }

            return MarkPendingOutcome.SKIPPED;
        }

        connectionService.updateConnectionStatus(connectionId, ConnectionStatus.PENDING_REASSIGNMENT);

        return MarkPendingOutcome.UPDATED;
    }

    @Override
    @AuditConnection(
        event = CONNECTION_REASSIGNED, connectionId = "#connectionId",
        data = @AuditData(key = "newOwnerLogin", value = "#newOwnerLogin"))
    public void reassignConnection(long workspaceId, long connectionId, String newOwnerLogin) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);
        validateUserExists(newOwnerLogin);

        reassignConnectionWithoutAudit(connectionId, newOwnerLogin);
    }

    @Override
    public void reassignAllConnections(long workspaceId, String userLogin, String newOwnerLogin) {
        validateUserExists(newOwnerLogin);

        List<ConnectionReassignmentItem> unresolvedConnections = getUnresolvedConnections(workspaceId, userLogin);

        // Build the audit list from rows that actually updated, not from the input. Today the method
        // is fully @Transactional so any per-row throw rolls back the whole batch and the distinction
        // is moot — but if a future refactor introduces REQUIRES_NEW per row (mirroring
        // promoteAllPrivateToWorkspace), audit emission must not claim success for rolled-back rows.
        List<Long> reassignedIds = new ArrayList<>(unresolvedConnections.size());

        for (ConnectionReassignmentItem item : unresolvedConnections) {
            reassignConnectionWithoutAudit(item.connectionId(), newOwnerLogin);

            reassignedIds.add(item.connectionId());
        }

        // Group per-row CONNECTION_REASSIGNED events under a single correlation ID so auditors can
        // reassemble which rows belong to the same bulk reassignment — mirroring the umbrella+children
        // pattern setConnectionProjects uses for CONNECTION_SHARES_REPLACED. The correlation ID is
        // attached to each event directly (passed into registerAfterCommitAudit) rather than via a
        // ThreadLocal scope, because the afterCommit callback fires after this method returns and a
        // try-with-resources scope would already be closed by that point.
        AuditCorrelation.CorrelationId correlationId = AuditCorrelation.newId();

        registerAfterCommitAudit(reassignedIds, newOwnerLogin, correlationId);
    }

    /**
     * Publishes CONNECTION_REASSIGNED events after the enclosing transaction commits. This matters because
     * {@link #reassignAllConnections} rewrites owner/status across multiple rows: if any row in the batch triggers a
     * rollback (constraint violation, optimistic-lock collision, listener failure), we must not have already emitted
     * audit events that claim the reassignment succeeded. Publishing at {@code afterCommit} guarantees the audit trail
     * mirrors committed state — never the intent that was reverted.
     *
     * <p>
     * The supplied {@code correlationId} groups every per-row event from the same bulk invocation, mirroring the
     * umbrella+children pattern {@code setConnectionProjects} uses.
     *
     * <p>
     * When no transaction synchronization is active (e.g. a caller that invoked this facade outside a Spring-managed
     * transaction) we publish immediately rather than silently dropping the events; the caller has already taken
     * responsibility for their own transactional boundary.
     */
    private void registerAfterCommitAudit(
        List<Long> connectionIds, String newOwnerLogin, AuditCorrelation.CorrelationId correlationId) {

        if (connectionIds.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // No surrounding transaction — emit eagerly. Still wrap per-row so one broken listener
            // does not drop the rest of the batch; the row has already been persisted.
            for (long connectionId : connectionIds) {
                publishReassignmentAudit(connectionId, newOwnerLogin, correlationId);
            }

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (long connectionId : connectionIds) {
                    publishReassignmentAudit(connectionId, newOwnerLogin, correlationId);
                }
            }
        });
    }

    /**
     * Emits a single {@code CONNECTION_REASSIGNED} audit event. Any publisher failure is logged at ERROR and counted
     * via {@code bytechef_connection_audit_failed} (the same metric {@link ConnectionAuditAspect} uses) so operators
     * see a unified view of audit-trail health across the annotation-driven and imperative emission paths.
     *
     * <p>
     * Catches {@link Exception} so a publisher failure in one row does not abort the batch. {@link Error} subtypes
     * (OOM, StackOverflowError, VirtualMachineError) are deliberately NOT caught — the JVM contract is that {@code
     * Error} signals unrecoverable conditions and must propagate; silencing them would let the loop keep allocating
     * through an OOM and mask catastrophic state.
     */
    private void publishReassignmentAudit(
        long connectionId, String newOwnerLogin, AuditCorrelation.CorrelationId correlationId) {

        try {
            connectionAuditPublisher.publish(
                CONNECTION_REASSIGNED, connectionId,
                Map.of(
                    "newOwnerLogin", newOwnerLogin,
                    "correlationId", correlationId.value()));
        } catch (Exception exception) {
            recordAuditFailure();

            logger.error("Failed to publish CONNECTION_REASSIGNED audit for id={}", connectionId, exception);
        }
    }

    private void reassignConnectionWithoutAudit(long connectionId, String newOwnerLogin) {
        // REVOKED is terminal by design (see ConnectionStatus#REVOKED). Changing ownership on a revoked
        // row would break the monotonic-audit invariant: an operator would see a "successfully
        // reassigned" event for a credential that should not have been transferable. Read-before-write
        // gives us a TOCTOU-tight enough guard — any concurrent revoker will observe the new owner but
        // will still revoke (REVOKED remains terminal after reassignment), which is the correct outcome.
        ConnectionStatus currentStatus = connectionService.getConnection(connectionId)
            .getStatus();

        if (currentStatus == ConnectionStatus.REVOKED) {
            throw new ConfigurationException(
                "Cannot reassign a revoked connection (id=" + connectionId + "); REVOKED is terminal",
                ConnectionErrorType.INVALID_CONNECTION);
        }

        Connection connection = connectionService.updateCreatedBy(connectionId, newOwnerLogin);

        if (connection.getStatus() == ConnectionStatus.PENDING_REASSIGNMENT) {
            connectionService.updateConnectionStatus(connectionId, ConnectionStatus.ACTIVE);
        }
    }

    private void validateConnectionBelongsToWorkspace(long workspaceId, long connectionId) {
        boolean belongs = workspaceConnectionService.getWorkspaceConnections(workspaceId)
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .anyMatch(id -> id == connectionId);

        if (!belongs) {
            throw new ConfigurationException(
                "Connection id=%s does not belong to workspace id=%s".formatted(connectionId, workspaceId),
                ConnectionErrorType.INVALID_CONNECTION);
        }
    }

    private void validateUserExists(String login) {
        userService.fetchUserByLogin(login)
            .orElseThrow(() -> new ConfigurationException(
                "User with login '%s' does not exist".formatted(login),
                ConnectionErrorType.INVALID_CONNECTION));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AffectedWorkflow> getAffectedWorkflows(long workspaceId, String userLogin) {
        List<Long> connectionIds = workspaceConnectionService.getWorkspaceConnections(workspaceId)
            .stream()
            .map(WorkspaceConnection::getConnectionId)
            .toList();

        if (connectionIds.isEmpty()) {
            return List.of();
        }

        Set<Long> userConnectionIds = connectionService.getConnections(connectionIds)
            .stream()
            .filter(connection -> userLogin.equals(connection.getCreatedBy()))
            .map(Connection::getId)
            .collect(Collectors.toSet());

        if (userConnectionIds.isEmpty()) {
            return List.of();
        }

        List<ProjectDeploymentWorkflow> deploymentWorkflows =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflowsByConnectionIds(
                new ArrayList<>(userConnectionIds));

        if (deploymentWorkflows.isEmpty()) {
            return List.of();
        }

        List<String> workflowIds = deploymentWorkflows.stream()
            .map(ProjectDeploymentWorkflow::getWorkflowId)
            .distinct()
            .toList();

        Map<String, String> workflowLabels = new HashMap<>();

        for (Workflow workflow : workflowService.getWorkflows(workflowIds)) {
            workflowLabels.put(workflow.getId(), workflow.getLabel());
        }

        Map<String, List<Long>> workflowConnectionIdsMap = new HashMap<>();

        for (ProjectDeploymentWorkflow deploymentWorkflow : deploymentWorkflows) {
            String workflowId = deploymentWorkflow.getWorkflowId();

            List<Long> usedConnectionIds = deploymentWorkflow.getConnections()
                .stream()
                .map(ProjectDeploymentWorkflowConnection::getConnectionId)
                .filter(userConnectionIds::contains)
                .toList();

            workflowConnectionIdsMap.computeIfAbsent(workflowId, key -> new ArrayList<>())
                .addAll(usedConnectionIds);
        }

        return workflowConnectionIdsMap.entrySet()
            .stream()
            .map(entry -> new AffectedWorkflow(
                entry.getKey(),
                workflowLabels.getOrDefault(entry.getKey(), entry.getKey()),
                entry.getValue()
                    .stream()
                    .distinct()
                    .toList()))
            .toList();
    }
}
