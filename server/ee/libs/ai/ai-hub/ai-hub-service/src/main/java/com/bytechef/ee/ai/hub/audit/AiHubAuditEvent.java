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
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubAuditEvent {

    /**
     * A personal agent was persisted. Payload: {@code workspaceId}, {@code agentId} ({@code #result.id}), {@code name}
     * ({@code #result.name}), {@code environment}.
     */
    AI_HUB_PERSONAL_AGENT_CREATED(false),

    /**
     * An existing personal agent's editable fields were updated. Payload: {@code workspaceId}, {@code agentId}.
     * Diff-level granularity (which fields changed) is intentionally omitted in v1 — the {@code update} method's
     * conditional patch logic isn't reachable from the aspect's arg/return SpEL context.
     */
    AI_HUB_PERSONAL_AGENT_UPDATED(false),

    /**
     * A personal agent was deleted. Payload: {@code workspaceId}, {@code agentId}.
     *
     * <p>
     * Marked {@code strictAudit} — deletion without a trail is the prototypical compliance blind spot. SpEL evaluation
     * failure here rethrows {@code AuditCaptureFailedException} so the surrounding {@code @Transactional} rolls back
     * rather than the agent disappearing without an audit row.
     */
    AI_HUB_PERSONAL_AGENT_DELETED(true),

    /**
     * A tool template was attached to an agent. Payload: {@code workspaceId}, {@code agentId}, {@code componentName},
     * {@code componentVersion}, {@code operationName}.
     */
    AI_HUB_PERSONAL_AGENT_TOOL_ADDED(false),

    /**
     * A tool template was detached from an agent. Payload: {@code workspaceId}, {@code toolId}. The parent {@code
     * agentId} is intentionally omitted in v1 — {@code removeTool} returns {@code void} and the row is gone by
     * {@code @AfterReturning} time, so the aspect cannot reach it. Audit consumers correlate via {@code toolId}.
     */
    AI_HUB_PERSONAL_AGENT_TOOL_REMOVED(false),

    /**
     * A tool template's pinned connection or pre-set parameters were updated. Payload: {@code workspaceId},
     * {@code agentId} ({@code #result.aiHubPersonalAgentId}), {@code toolId}, {@code connectionId} (nullable),
     * {@code parameterKeys} (string rendering of the parameter map's key set, e.g. {@code "[a, b, c]"}).
     */
    AI_HUB_PERSONAL_AGENT_TOOL_CONFIG_UPDATED(false),

    /**
     * A schedule was inserted or updated. Payload: {@code workspaceId}, {@code agentId}, {@code scheduleId},
     * {@code enabled}, {@code frequencyKind}, {@code effectiveCronExpression}. Emitted imperatively from
     * {@code AiHubPersonalAgentScheduleServiceImpl.upsertOrDelete} (not via the aspect) because one method emits two
     * different event types depending on the input-null branch.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_UPSERTED(false),

    /**
     * A schedule was removed. Payload: {@code workspaceId}, {@code agentId}, {@code scheduleId}. Emitted imperatively
     * from {@code upsertOrDelete}'s delete branch.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_DELETED(false),

    /**
     * A scheduled fire produced a new task. Payload: {@code workspaceId}, {@code agentId}, {@code scheduleId},
     * {@code taskId}. Emitted from {@code AgentScheduleFiredEventListener.onFired} on the Quartz thread; principal
     * falls back to {@code "SYSTEM"} via {@link AiHubAuditPublisher}.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_FIRED(false),

    /**
     * A schedule was auto-disabled after three consecutive failures. Payload: {@code workspaceId}, {@code agentId},
     * {@code scheduleId}, {@code reason} (currently always {@code "three_consecutive_failures"}). Emitted imperatively
     * from {@code AiHubPersonalAgentScheduleServiceImpl.recordFailure} in the threshold branch.
     */
    AI_HUB_PERSONAL_AGENT_SCHEDULE_AUTO_DISABLED(false),

    /**
     * Workspace-level AI Hub settings changed. Payload: {@code workspaceId}, {@code changedFields} (the literal name of
     * the changed field group, e.g. {@code "voiceWebhookUrl"} or {@code "voiceProvider"}).
     */
    AI_HUB_WORKSPACE_SETTINGS_UPDATED(false),

    /**
     * A resource template was attached to an agent. Payload: {@code workspaceId}, {@code agentId}, {@code kind},
     * {@code resourceId}.
     */
    AI_HUB_PERSONAL_AGENT_RESOURCE_ADDED(false),

    /**
     * A resource template was detached from an agent. Payload: {@code workspaceId}, {@code personalAgentResourceId}.
     * The parent {@code agentId} is omitted — {@code removeResource} returns {@code void} and the row is gone by
     * {@code @AfterReturning} time.
     */
    AI_HUB_PERSONAL_AGENT_RESOURCE_REMOVED(false),

    /**
     * A task was created (standard chat, workflow chat, or personal-agent chat). Payload: {@code taskId},
     * {@code threadId}, {@code kind}, {@code environment}. Emitted imperatively from {@code AiHubTaskServiceImpl}'s
     * create paths after the row persists. Low-frequency (one per task), so it does not flood the audit log the way
     * per-turn agent activity would.
     */
    AI_HUB_TASK_CREATED(false),

    /**
     * A task was deleted, irreversibly removing its chat history. Payload: {@code taskId}, {@code threadId}. Emitted
     * imperatively from {@code AiHubTaskServiceImpl.delete} after the row is removed.
     *
     * <p>
     * Marked {@code strictAudit} — a task deletion drops the entire conversation transcript, so a missing audit trail
     * is a compliance blind spot.
     */
    AI_HUB_TASK_DELETED(true);

    private final boolean strictAudit;

    AiHubAuditEvent(boolean strictAudit) {
        this.strictAudit = strictAudit;
    }

    public boolean isStrictAudit() {
        return strictAudit;
    }
}
