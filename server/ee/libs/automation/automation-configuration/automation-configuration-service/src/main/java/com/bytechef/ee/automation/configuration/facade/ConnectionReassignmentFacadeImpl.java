/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.WorkspaceConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.ee.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.ee.automation.configuration.dto.BulkReassignResultDTO.BulkReassignFailureDTO;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class ConnectionReassignmentFacadeImpl implements ConnectionReassignmentFacade {

    private static final Logger log = LoggerFactory.getLogger(ConnectionReassignmentFacadeImpl.class);

    private final ConnectionService connectionService;
    private final MeterRegistry meterRegistry;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final WorkspaceConnectionService workspaceConnectionService;

    // Self-reference via the interface so AOP advice (@Transactional propagation for the per-row
    // REQUIRES_NEW worker) fires on internal re-entry (bulk mark-pending → markSingleConnectionPendingReassignment).
    // @Lazy breaks the circular dependency Spring would otherwise refuse.
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
    public ConnectionReassignmentFacadeImpl(ConnectionService connectionService,
        ObjectProvider<MeterRegistry> meterRegistryProvider,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, UserService userService,
        WorkflowService workflowService, WorkspaceConnectionService workspaceConnectionService) {

        this.connectionService = connectionService;
        this.meterRegistry = meterRegistryProvider.getIfAvailable();
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.userService = userService;
        this.workflowService = workflowService;
        this.workspaceConnectionService = workspaceConnectionService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public BulkReassignResultDTO markConnectionsPendingReassignmentAsAdmin(long workspaceId, String userLogin) {
        return markConnectionsPendingReassignment(workspaceId, userLogin);
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

                log.warn(
                    "Failed to mark connection id={} as PENDING_REASSIGNMENT for workspace={} user={}; continuing",
                    item.connectionId(), workspaceId, userLogin, configurationException);
            } catch (RuntimeException exception) {
                // Sanitize unknown exceptions — never forward raw JDBC / SQL detail into an admin toast.
                failures.add(BulkReassignFailureDTO.of(
                    item.connectionId(), BulkReassignFailureDTO.UNEXPECTED_ERROR_CODE,
                    "Unexpected error: " + exception.getClass()
                        .getSimpleName()));

                log.error(
                    "Unexpected failure marking connection id={} as PENDING_REASSIGNMENT for workspace={} user={};"
                        + " continuing",
                    item.connectionId(), workspaceId, userLogin, exception);
            }
        }

        int failed = failures.size();

        if (failed > 0) {
            log.error(
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
        // Re-read current status inside the REQUIRES_NEW tx so a row whose status changed between the enumeration
        // and this per-row call is classified on what it is now. Rehydrated from a primitive INT code by
        // Connection.getStatus, so no null-guard.
        ConnectionStatus currentStatus = connectionService.getConnection(connectionId)
            .getStatus();

        if (!currentStatus.canTransitionTo(ConnectionStatus.PENDING_REASSIGNMENT)) {
            if (log.isInfoEnabled()) {
                log.info(
                    "Skipping connection id={}: status={} cannot transition to PENDING_REASSIGNMENT",
                    connectionId, currentStatus);
            }

            return MarkPendingOutcome.SKIPPED;
        }

        connectionService.updateConnectionStatus(connectionId, ConnectionStatus.PENDING_REASSIGNMENT);

        return MarkPendingOutcome.UPDATED;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void reassignConnection(long workspaceId, long connectionId, String newOwnerLogin) {
        validateConnectionBelongsToWorkspace(workspaceId, connectionId);
        validateUserExists(newOwnerLogin);

        applyReassignment(connectionId, newOwnerLogin);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void reassignAllConnections(long workspaceId, String userLogin, String newOwnerLogin) {
        validateUserExists(newOwnerLogin);

        List<ConnectionReassignmentItem> unresolvedConnections = getUnresolvedConnections(workspaceId, userLogin);

        for (ConnectionReassignmentItem item : unresolvedConnections) {
            applyReassignment(item.connectionId(), newOwnerLogin);
        }
    }

    private void applyReassignment(long connectionId, String newOwnerLogin) {
        // REVOKED is terminal by design (see ConnectionStatus#REVOKED): a credential that should not have been
        // transferable must not change hands.
        ConnectionStatus currentStatus = connectionService.getConnection(connectionId)
            .getStatus();

        if (currentStatus == ConnectionStatus.REVOKED) {
            throw new ConfigurationException(
                "Cannot reassign a revoked connection (id=" + connectionId + "); REVOKED is terminal",
                ConnectionErrorType.INVALID_CONNECTION);
        }

        connectionService.reassignOwner(connectionId, newOwnerLogin);
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
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
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
