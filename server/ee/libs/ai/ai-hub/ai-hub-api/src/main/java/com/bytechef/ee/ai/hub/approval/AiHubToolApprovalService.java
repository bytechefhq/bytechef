/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.util.List;
import java.util.Optional;

/**
 * CRUD and lifecycle operations over {@link AiHubToolApproval} rows. The ai-hub-service approval gate tool callback
 * wrapper is the primary writer of {@code PENDING} rows via {@link #createPending(AiHubToolApproval)}; the
 * approval-decision facade reads and resolves them through the remaining methods.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubToolApprovalService {

    /**
     * Persists a new {@code PENDING} approval. Stamps {@link AiHubToolApproval#getExpiresAt()} to 24 hours from now
     * when the caller left it unset.
     */
    AiHubToolApproval createPending(AiHubToolApproval approval);

    /**
     * Returns the chat's currently pending approval, if any. A chat has at most one pending approval at a time — a
     * second gated tool call while one is outstanding is deferred rather than creating a second row.
     */
    Optional<AiHubToolApproval> findPending(long chatId);

    /**
     * Lists every approval ever recorded for the chat, most recently created first.
     */
    List<AiHubToolApproval> list(long chatId);

    /**
     * Loads a single approval by id. Throws {@code NotFoundException} when no such row exists.
     */
    AiHubToolApproval get(long approvalId);

    /**
     * Persists a mutated approval (e.g. after a decision has been applied to it).
     */
    AiHubToolApproval save(AiHubToolApproval approval);

    /**
     * Marks every currently {@code PENDING} approval for the chat as {@code SUPERSEDED}. Returns the number of rows
     * changed.
     */
    int supersedePending(long chatId);

    /**
     * Deletes every approval recorded for the chat. Called from the chat's own delete path, since the approval table
     * carries no foreign key to the chat.
     */
    void deleteByChat(long chatId);
}
