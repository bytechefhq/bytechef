/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Resolution surface over pending {@link AiHubToolApproval} rows: listing a chat's approval history, and applying a
 * person's approve/reject decision to a {@code PENDING} row. Authorization is chat-ownership-based (the chat owner, or
 * a user holding the tenant {@code ADMIN} authority), enforced through the chat service's access checks rather than a
 * workspace role: a tool approval belongs to one chat, not to the workspace at large.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubToolApprovalFacade {

    /**
     * Lists every approval ever recorded for {@code chatId}, most recently created first. Throws when {@code chatId}
     * does not resolve to a chat the current user can view within {@code workspaceId}.
     */
    List<AiHubToolApproval> list(long workspaceId, long chatId);

    /**
     * Applies a person's decision to a {@code PENDING} approval: records who decided and when, executes the stored tool
     * call when {@code approved}, then starts a continuation turn on the owning chat so the conversation resumes with
     * the outcome. The decision is committed before the tool runs and before the continuation turn starts.
     */
    Resolution resolve(long workspaceId, long approvalId, boolean approved, @Nullable String comment);

    /**
     * The outcome of a {@link #resolve} call: the decided (and possibly executed) approval row, whether a continuation
     * turn was actually started, and that turn's run id when it was.
     *
     * <p>
     * {@code approval} is deliberately the live {@link AiHubToolApproval} row rather than a copy: it is a Spring Data
     * JDBC entity with no copy path, and every caller wants the decided row's own state. The {@code EI} suppression
     * records that this is the intended shape.
     * </p>
     */
    @SuppressFBWarnings("EI")
    record Resolution(AiHubToolApproval approval, boolean continuationStarted, @Nullable String runId) {
    }
}
