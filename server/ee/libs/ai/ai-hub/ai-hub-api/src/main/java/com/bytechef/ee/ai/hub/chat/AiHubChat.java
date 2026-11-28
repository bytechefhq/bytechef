/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent metadata record for a AI Hub chat. Message contents are stored separately in Spring AI's session store
 * (tables {@code AI_SESSION} / {@code AI_SESSION_EVENT} under the default jdbc backend) and keyed by {@link #threadId},
 * which is used verbatim as the session id.
 *
 * <p>
 * This is a Spring Data JDBC row-mapper entity. Setters are intentionally permissive; Spring Data JDBC reflects
 * directly on fields on load, so they exist for the application-side construct-then-save flow. Ownership and
 * authorization invariants (workspace ownership, user ownership, status transitions) are enforced in
 * {@code AiHubChatServiceImpl}, not on this type. Do not call {@link #setUserId(long)} or {@link #setWorkspaceId(Long)}
 * on a loaded row to "retarget" it; use a service.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_chat")
public class AiHubChat {

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

    @Column("auto_titled")
    private boolean autoTitled = true;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    public AiHubChat() {
    }

    public AiHubChat(long userId) {
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
                "AiHubChat.messageCount must be >= 0 (got " + messageCount + ")");
        }

        this.messageCount = messageCount;
    }

    public AiHubChatStatus getStatus() {
        return EnumOrdinals.fromOrdinal(status, AiHubChatStatus.class);
    }

    public void setStatus(AiHubChatStatus status) {
        Objects.requireNonNull(status, "status");

        AiHubChatStatus current =
            EnumOrdinals.fromOrdinal(this.status, AiHubChatStatus.class);

        if (current == AiHubChatStatus.DELETED && status != AiHubChatStatus.DELETED) {
            throw new IllegalStateException(
                "Cannot transition AiHubChat from DELETED to " + status +
                    "; deletion is terminal. Create a new chat instead.");
        }

        this.status = status.ordinal();
    }

    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
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

        AiHubChat that = (AiHubChat) other;

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
        return "AiHubChat{" +
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
