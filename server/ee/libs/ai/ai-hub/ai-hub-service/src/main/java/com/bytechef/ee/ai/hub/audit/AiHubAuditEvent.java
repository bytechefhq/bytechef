/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.audit;

/**
 * Audit event types emitted through {@link AiHubAuditPublisher} for material durable-state mutations on AI Hub
 * resources. Every event carries {@code workspaceId} in its payload; additional required keys are documented per
 * constant. The payload contract is convention-enforced — changes must be applied at every emitter.
 *
 * <p>
 * {@code strictAudit = true} means an SpEL-evaluation failure during {@link AiHubAuditAspect} capture rolls back the
 * surrounding business transaction rather than absorbing the failure into the counter. Reserved for compliance-grade
 * events where a missing trail is itself a regression.
 *
 * <p>
 * <b>Recycled names — read before querying the trail.</b> {@link AiHubAuditPublisher} persists the constant by
 * {@link #name()} as an opaque string into {@code persistent_audit_event}, so the stored value is whatever the enum was
 * called when the row was written, not what it is called now. These names were recycled during the
 * conversation&#8594;chat and template&#8594;task rename: before that rename {@code AI_HUB_TASK_CREATED} /
 * {@code AI_HUB_TASK_DELETED} were emitted for CONVERSATION create/delete, which today's {@code AI_HUB_CHAT_CREATED} /
 * {@code AI_HUB_CHAT_DELETED} cover. Rows written by a build predating the rename therefore carry {@code AI_HUB_TASK_*}
 * while meaning chats. Only pre-release environments hold affected rows, so no migration is warranted; a consumer
 * reading a long-lived pre-release trail should disambiguate by row timestamp.
 *
 * <p>
 * The {@code AI_HUB_TASK_*} constants themselves no longer exist — the AI Hub task domain was removed in favour of AI
 * Agents, taking every emitter with it. Historical rows written by those emitters remain readable as opaque strings,
 * and their payload key {@code agentId} carries the AI Hub <em>task</em> id rather than an agent id.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubAuditEvent {

    /**
     * Workspace-level AI Hub settings changed. Payload: {@code workspaceId}, {@code changedFields} (the literal name of
     * the changed field group, e.g. {@code "voiceWebhookUrl"} or {@code "voiceProvider"}).
     */
    AI_HUB_WORKSPACE_SETTINGS_UPDATED(false),

    /**
     * A chat was created (standard chat, workflow chat, or agent chat). Payload: {@code chatId}, {@code threadId},
     * {@code kind}, {@code environment}. Emitted imperatively from {@code AiHubChatServiceImpl}'s create paths after
     * the row persists. Low-frequency (one per chat), so it does not flood the audit log the way per-turn agent
     * activity would.
     */
    AI_HUB_CHAT_CREATED(false),

    /**
     * A chat was deleted, irreversibly removing its chat history. Payload: {@code chatId}, {@code threadId}. Emitted
     * imperatively from {@code AiHubChatServiceImpl.delete} after the row is removed.
     *
     * <p>
     * Marked {@code strictAudit} — a chat deletion drops the entire conversation transcript, so a missing audit trail
     * is a compliance blind spot.
     */
    AI_HUB_CHAT_DELETED(true);

    private final boolean strictAudit;

    AiHubAuditEvent(boolean strictAudit) {
        this.strictAudit = strictAudit;
    }

    public boolean isStrictAudit() {
        return strictAudit;
    }
}
