/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent metadata record for a AI Hub task. Message contents are stored separately in Spring AI's session store
 * (tables {@code AI_SESSION} / {@code AI_SESSION_EVENT} under the default jdbc backend) and keyed by {@link #threadId},
 * which is used verbatim as the session id.
 *
 * <p>
 * This is a Spring Data JDBC row-mapper entity. Setters are intentionally permissive — Spring Data JDBC reflects
 * directly on fields on load, so they exist for the application-side construct-then-save flow. Ownership and
 * authorization invariants (workspace ownership, user ownership, status transitions) are enforced in
 * {@code AiHubTaskServiceImpl}, not on this type. Do not call {@link #setUserId(long)} or {@link #setWorkspaceId(Long)}
 * on a loaded row to "retarget" it; use a service.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_task")
public class AiHubTask {

    @Id
    private Long id;

    @Column("user_id")
    private long userId;

    @Column("thread_id")
    private String threadId;

    @Column("title")
    private String title;

    @Column("last_preview")
    private String lastPreview;

    @Column("message_count")
    private int messageCount;

    @Column("status")
    private int status;

    @Column("environment")
    private int environment;

    /**
     * INT ordinal of {@link AiHubTaskKind} — discriminates standard tasks (default, {@code STANDARD}) from
     * workflow-chat tasks ({@code WORKFLOW_CHAT}). Ordinal stability enforced by {@code EnumOrdinalStabilityTest}; new
     * kinds must be appended at the end of the enum.
     */
    @Column("kind")
    private int kind = AiHubTaskKind.STANDARD.ordinal();

    /**
     * Composite WorkflowExecutionId string (tenantId + UUID) the task chats with. Non-null for
     * {@code kind = WORKFLOW_CHAT}; null for {@code STANDARD}. Drives the server-side adapter that bypasses the LLM
     * agent and routes per-turn invocations through the webhook executor.
     */
    @Column("workflow_execution_id")
    private String workflowExecutionId;

    /**
     * Parent project-deployment id for the workflow execution. Surfaces sidebar grouping ("workflows under Project A")
     * without forcing a JOIN at render time. Non-null for {@code kind = WORKFLOW_CHAT}; null for {@code STANDARD}.
     */
    @Column("project_deployment_id")
    private Long projectDeploymentId;

    /**
     * Owning personal-agent id for {@code kind = PERSONAL_AGENT} rows. Drives the system-prompt extension applied at
     * dispatch time and the sidebar grouping under the agent. Non-null for {@code PERSONAL_AGENT} rows; null for
     * {@code STANDARD} and {@code WORKFLOW_CHAT}. Mirrors the {@code workflow_execution_id} pattern: kind-specific
     * column nullable for the kinds that don't use it. Multiple rows can share the same
     * {@code ai_hub_personal_agent_id} under always-new task semantics — there is no partial unique index scoping rows
     * per {@code (workspace, user, environment, ai_hub_personal_agent_id)}.
     */
    @Column("ai_hub_personal_agent_id")
    private Long aiHubPersonalAgentId;

    /**
     * Whether the current title was set automatically (creation-time auto-label or LLM regeneration) and is therefore
     * eligible for further automatic regeneration. The semantic is "has never been authoritatively titled" — once the
     * LLM regenerates a title, this flag flips to {@code false} so the regen loop doesn't fire again per turn. A user
     * patch that sets a title also flips it false. Workflow chats start at {@code true} so their initial
     * "${projectName} — ${workflowLabel}" label can be replaced by an LLM-generated title once enough task accumulates.
     */
    @Column("auto_titled")
    private boolean autoTitled = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Workspace the task belongs to. Nullable: a task belongs to at most one workspace, and null means none applies.
     * Workspace-scoped queries never match a null, so a workspace-less task is invisible to them.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    public AiHubTask() {
    }

    /**
     * Bind-once constructor for new (unpersisted) rows. The per-field setter for {@code userId} is package-private so a
     * loaded row cannot be retargeted by mistake. The workspace association is the separately settable
     * {@link #workspaceId} column — callers that only have a task ID still go through the service helper
     * {@code AiHubTaskService.getWorkspaceId(taskId)}.
     */
    public AiHubTask(long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastPreview() {
        return lastPreview;
    }

    public void setLastPreview(String lastPreview) {
        this.lastPreview = lastPreview;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        if (messageCount < 0) {
            throw new IllegalArgumentException(
                "AiHubTask.messageCount must be >= 0 (got " + messageCount + ")");
        }

        this.messageCount = messageCount;
    }

    public AiHubTaskStatus getStatus() {
        return EnumOrdinals.fromOrdinal(status, AiHubTaskStatus.class);
    }

    /**
     * Updates the lifecycle status of this task. Forbids transitioning out of {@link AiHubTaskStatus#DELETED} —
     * deletion is terminal and a "resurrection" via raw setStatus would silently defeat the audit trail (no record of
     * when or why the row came back). Application code that genuinely needs to recover a task should create a new row.
     *
     * <p>
     * Spring Data JDBC reflects directly on the {@code status} field during hydration, so this guard only fires for
     * application-side mutations — load is unaffected.
     * </p>
     *
     * @throws NullPointerException  when {@code status} is null
     * @throws IllegalStateException when this task's current status is {@code DELETED} and {@code status} would change
     *                               it to anything else
     */
    public void setStatus(AiHubTaskStatus status) {
        Objects.requireNonNull(status, "status");

        AiHubTaskStatus current =
            EnumOrdinals.fromOrdinal(this.status, AiHubTaskStatus.class);

        if (current == AiHubTaskStatus.DELETED && status != AiHubTaskStatus.DELETED) {
            throw new IllegalStateException(
                "Cannot transition AiHubTask from DELETED to " + status +
                    "; deletion is terminal. Create a new task instead.");
        }

        this.status = status.ordinal();
    }

    /**
     * Returns the {@link Environment} this task belongs to. Bounds-checked to surface a corrupt ordinal as a typed
     * {@link IllegalStateException} instead of an opaque {@code ArrayIndexOutOfBoundsException}.
     */
    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    /**
     * Returns the raw ordinal as a long, used by REST/GraphQL surfaces that round-trip the value as
     * {@code environmentId}.
     */
    public long getEnvironmentId() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public AiHubTaskKind getKind() {
        return EnumOrdinals.fromOrdinal(kind, AiHubTaskKind.class);
    }

    public void setKind(AiHubTaskKind kind) {
        if (kind != null) {
            this.kind = kind.ordinal();
        }
    }

    public String getWorkflowExecutionId() {
        return workflowExecutionId;
    }

    public void setWorkflowExecutionId(String workflowExecutionId) {
        this.workflowExecutionId = workflowExecutionId;
    }

    public Long getProjectDeploymentId() {
        return projectDeploymentId;
    }

    public void setProjectDeploymentId(Long projectDeploymentId) {
        this.projectDeploymentId = projectDeploymentId;
    }

    public @Nullable Long getAiHubPersonalAgentId() {
        return aiHubPersonalAgentId;
    }

    public void setAiHubPersonalAgentId(@Nullable Long aiHubPersonalAgentId) {
        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
    }

    public boolean isAutoTitled() {
        return autoTitled;
    }

    public void setAutoTitled(boolean autoTitled) {
        this.autoTitled = autoTitled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiHubTask that = (AiHubTask) other;

        // Two transient (id == null) instances must not collide in a Set before persistence.
        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AiHubTask{" +
            "id=" + id +
            ", userId=" + userId +
            ", threadId='" + threadId + '\'' +
            ", title='" + title + '\'' +
            ", status=" + status +
            ", environment=" + environment +
            ", messageCount=" + messageCount +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}
