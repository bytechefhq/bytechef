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

import com.bytechef.automation.configuration.dto.BulkReassignResultDTO;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import java.util.List;

/**
 * Facade for the orphan-recovery flow triggered when a workspace user is removed. Exposes the admin-driven bulk and
 * per-row moves that return a removed user's credentials to an active owner without losing audit continuity.
 *
 * <p>
 * <b>Authorization.</b> All mutations are {@code @PreAuthorize(ROLE_ADMIN)} at the GraphQL controller — the facade
 * assumes the caller has already passed that check. Queries exposed here are likewise admin-only.
 *
 * <p>
 * <b>Audit emission.</b> Single-row {@code reassignConnection} uses annotation-driven audit via
 * {@code @AuditConnection}. {@code reassignAllConnections} batches per-row work under a single transaction and emits
 * audit events imperatively after the outer commit so claims of "successfully reassigned" cannot survive a rollback.
 *
 * <p>
 * <b>Partial-failure surfacing.</b> {@code markConnectionsPendingReassignment} returns a
 * {@link com.bytechef.automation.configuration.dto.BulkReassignResultDTO} with separate {@code updated} /
 * {@code skipped} / {@code failed} counts plus per-row {@code failures}. Benign terminal states (e.g. REVOKED) are
 * classified as {@code skipped} so a silent no-op does not read as an error.
 *
 * @author Ivica Cardic
 */
public interface ConnectionReassignmentFacade {

    List<ConnectionReassignmentItem> getUnresolvedConnections(long workspaceId, String userLogin);

    /**
     * Marks every connection owned by {@code userLogin} in the given workspace as {@code PENDING_REASSIGNMENT}. The
     * returned {@link BulkReassignResultDTO} reports {@code updated} / {@code skipped} / {@code failed} counts
     * separately: rows already in a terminal state (e.g. {@code REVOKED}) are counted as {@code skipped} rather than
     * {@code failed}, so a silent no-op batch does not look like an error. Callers (notably the workspace user-removal
     * listener) can log / alert specifically on {@code failed > 0}.
     */
    BulkReassignResultDTO markConnectionsPendingReassignment(long workspaceId, String userLogin);

    /**
     * Per-row worker invoked through the AOP proxy by {@link #markConnectionsPendingReassignment} so each row commits
     * or rolls back in its own {@code REQUIRES_NEW} transaction. Returns {@link MarkPendingOutcome#UPDATED} when the
     * row's status successfully transitioned to {@code PENDING_REASSIGNMENT}, or {@link MarkPendingOutcome#SKIPPED}
     * when the row was in a terminal state (e.g. {@code REVOKED}) at read time and could not legally transition.
     *
     * <p>
     * Exposed on the interface because Spring-generated proxies only advise interface methods, and the per-row tx
     * boundary is load-bearing — without it, a single failing row would flip the outer transaction to rollback-only and
     * discard every previously-updated row in the same batch.
     */
    MarkPendingOutcome markSingleConnectionPendingReassignment(long connectionId);

    void reassignConnection(long workspaceId, long connectionId, String newOwnerLogin);

    void reassignAllConnections(long workspaceId, String userLogin, String newOwnerLogin);

    List<AffectedWorkflow> getAffectedWorkflows(long workspaceId, String userLogin);

    /**
     * Outcome of a per-row {@link #markSingleConnectionPendingReassignment} call. A {@code SKIPPED} value is a benign
     * no-op (row cannot transition from its current terminal state); a real failure throws a {@link RuntimeException}
     * out of the method and the bulk loop records it as {@code failed}, never {@code skipped}.
     */
    enum MarkPendingOutcome {
        UPDATED,
        SKIPPED
    }

    record ConnectionReassignmentItem(
        long connectionId, String connectionName, ConnectionVisibility visibility, int environmentId,
        int dependentWorkflowCount) {
    }

    record AffectedWorkflow(String workflowId, String workflowName, List<Long> connectionIds) {
    }
}
