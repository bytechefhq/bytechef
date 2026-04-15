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

import com.bytechef.automation.configuration.dto.BulkPromoteResultDTO;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import java.util.List;

/**
 * Workspace-scoped connection operations. Transitions are coordinated here rather than on {@code ConnectionFacade} so
 * that workspace membership, project-share state, and audit/metric emission stay in one place.
 *
 * <p>
 * <b>Authorization model:</b> most mutations require {@code ROLE_ADMIN} (enforced by {@code @PreAuthorize} on the
 * GraphQL controller). The exception is {@link #demoteToPrivate(long, long)} — an admin OR the connection creator may
 * call it, to support the "all admins lost role" orphan-recovery path. The creator-as-fallback check is enforced inside
 * the facade (no {@code @PreAuthorize} on that mutation).
 *
 * <p>
 * <b>Bulk-operation semantics:</b> {@link #promoteAllPrivateToWorkspace(long)} returns a {@link BulkPromoteResultDTO}
 * with distinct counts for <i>promoted</i> (this call), <i>skipped</i> (benign concurrent race — row already at the
 * target visibility, carried as {@code CONNECTION_ALREADY_AT_TARGET_VISIBILITY}), and <i>failed</i> (real errors, each
 * with an entry in {@code failures}). Callers surface partial success rather than bailing on the first error.
 *
 * <p>
 * <b>Share-list idempotence:</b> {@link #shareConnectionToProject(long, long, long)} treats a double-share as a no-op
 * rather than surfacing the raw unique-constraint violation. {@link #setConnectionProjects(long, long, List)} is
 * diff-based (adds new, revokes removed) and wrapped in a {@code CONNECTION_SHARES_REPLACED} audit event that
 * establishes a correlation ID grouping the per-row share/revoke events.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceConnectionFacade {

    long create(long workspaceId, ConnectionDTO connectionDTO);

    void delete(long connectionId);

    void disconnectConnection(long connectionId);

    /**
     * Demote any-visibility connection back to PRIVATE. Admin OR creator (orphan-recovery) — authorization is enforced
     * before any existence/usage validation so unauthorized callers cannot probe the namespace via error-message
     * differences. Blocks if the connection is still used by an active deployment.
     */
    void demoteToPrivate(long workspaceId, long connectionId);

    List<ConnectionDTO> getConnections(
        long workspaceId, String componentName, Integer connectionVersion, Long environmentId, Long tagId);

    ConnectionVisibility promoteToWorkspace(long workspaceId, long connectionId);

    BulkPromoteResultDTO promoteAllPrivateToWorkspace(long workspaceId);

    void revokeConnectionFromProject(long workspaceId, long connectionId, long projectId);

    /**
     * Per-row revoke used by {@link #setConnectionProjects(long, long, List)} so the Spring AOP proxy fires an
     * {@code @AuditConnection} event for each row in a diff-based bulk replace. The name carries the {@code AuditOnly}
     * suffix to make its narrow purpose visible at call sites — it bypasses the per-row deployment-in-use guard (the
     * caller evaluates that once against the net result) and does not auto-demote to PRIVATE (the end-state visibility
     * is resolved once after all diff steps run).
     *
     * <p>
     * Not a general-purpose revoke. Direct callers MUST use {@link #revokeConnectionFromProject}.
     */
    void revokeSingleProjectShareAuditOnly(long connectionId, long projectId);

    void shareConnectionToProject(long workspaceId, long connectionId, long projectId);

    /**
     * Replace the project-share list for {@code connectionId} with exactly {@code projectIds} (diff-based). Shares are
     * added first, revokes happen last, so concurrent readers never see a transient PRIVATE state when projects are
     * swapped. The per-row deployment-in-use guard is bypassed — instead a single up-front check blocks the call only
     * when {@code projectIds} is empty AND the connection is still used by a deployment (which would orphan it).
     *
     * <p>
     * Wrapped in a {@code CONNECTION_SHARES_REPLACED} audit event with {@code establishCorrelation = true} so the
     * umbrella event and the per-row share/revoke events share one correlation ID.
     */
    void setConnectionProjects(long workspaceId, long connectionId, List<Long> projectIds);
}
