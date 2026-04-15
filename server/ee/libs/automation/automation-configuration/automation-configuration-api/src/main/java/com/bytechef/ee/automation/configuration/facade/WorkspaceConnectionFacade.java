/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * EE-only workspace connection visibility and sharing. Extends the CE base contract so the EE impl is the active bean
 * for both interfaces in EE and transparently satisfies CE consumers (CE REST/GraphQL).
 *
 * <p>
 * <b>Authorization model:</b> every mutation here is owner-or-admin, expressed in SpEL on the implementation. Unlike
 * the promote/demote surface this replaces, none of them needs the annotation deliberately <em>absent</em>: an admin
 * always satisfies the second disjunct, so there is no orphan-recovery hole to keep open by checking programmatically.
 *
 * <p>
 * <b>Non-disclosure:</b> a caller who may not manage a connection must not be able to tell "does not exist" from "not
 * yours" from "in use" by comparing error responses, which would let them probe the {@code (workspaceId,
 * connectionId)} namespace without being allowed to act on it. Authorization is therefore checked before any existence
 * or usage validation, and the failures collapse to one error type.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("NM")
public interface WorkspaceConnectionFacade
    extends com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade {

    /**
     * Sets the connection's reach. Rejects a rung the connection type does not support, and rejects
     * {@code ORGANIZATION}, which is reached through {@code OrganizationConnectionFacade} rather than here.
     *
     * <p>
     * Narrowing to {@code PRIVATE} is blocked while an active deployment uses the connection: withdrawing it would
     * break a running deployment. Widening carries no such risk and does not pay for the check.
     */
    void setConnectionVisibility(long workspaceId, long connectionId, ResourceVisibility visibility);

    /**
     * Grants a named user access to a connection its owner has withheld. Idempotent. The recipient must be a member of
     * the connection's workspace — a non-member is rejected without revealing whether their user id exists.
     */
    void grantConnectionAccess(long workspaceId, long connectionId, long userId);

    /**
     * Revokes a grant. Silent when no grant exists.
     */
    void revokeConnectionAccess(long workspaceId, long connectionId, long userId);

    /**
     * The users currently granted the connection. Owner-or-admin only: an ordinary viewer of a shared connection must
     * not learn who else it was handed to.
     */
    List<Long> getConnectionGrants(long workspaceId, long connectionId);
}
