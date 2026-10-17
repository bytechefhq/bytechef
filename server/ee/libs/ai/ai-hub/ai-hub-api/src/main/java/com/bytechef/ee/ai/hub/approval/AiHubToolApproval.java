/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.approval;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One flagged AI Hub tool call waiting for (or already resolved by) a person's approval decision. {@link #toolKind}
 * distinguishes a catalog tool from a component action; {@link #componentName}/{@link #componentVersion} are set only
 * for the latter. {@link #arguments} carries the LLM-supplied JSON call arguments verbatim, so a resolved approval can
 * re-drive the original tool invocation.
 *
 * <p>
 * No foreign key to {@code ai_hub_chat} — the chat's delete path deletes approvals explicitly, matching how
 * {@code ai_hub_chat_tool} rows are cleaned.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_tool_approval")
public final class AiHubToolApproval {

    public enum Status {
        PENDING, APPROVED, REJECTED, EXPIRED, SUPERSEDED, FAILED
    }

    public enum ToolKind {
        CATALOG, COMPONENT
    }

    @Id
    private Long id;

    @Column("chat_id")
    private long chatId;

    @Column("thread_id")
    private String threadId;

    @Column("requested_by_user_id")
    private long requestedByUserId;

    @Column("tool_kind")
    private int toolKind;

    @Column("tool_name")
    private String toolName;

    @Column("component_name")
    private @Nullable String componentName;

    @Column("component_version")
    private @Nullable Integer componentVersion;

    @Column("connection_id")
    private @Nullable Long connectionId;

    @Column("arguments")
    private String arguments;

    @Column
    private int status;

    @Column
    private String mode;

    @Column
    private int environment;

    @Column("llm_provider")
    private @Nullable String llmProvider;

    @Column("llm_model")
    private @Nullable String llmModel;

    @Column("decided_by_user_id")
    private @Nullable Long decidedByUserId;

    @Column("decided_at")
    private @Nullable Instant decidedAt;

    @Column
    private @Nullable String comment;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("execution_error")
    private @Nullable String executionError;

    @Column("created_by")
    @CreatedBy
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Version
    private int version;

    public AiHubToolApproval() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getThreadId() {
        return threadId;
    }

    public void setThreadId(String threadId) {
        this.threadId = threadId;
    }

    public long getRequestedByUserId() {
        return requestedByUserId;
    }

    public void setRequestedByUserId(long requestedByUserId) {
        this.requestedByUserId = requestedByUserId;
    }

    public ToolKind getToolKind() {
        return ToolKind.values()[toolKind];
    }

    public void setToolKind(ToolKind toolKind) {
        this.toolKind = toolKind.ordinal();
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public @Nullable String getComponentName() {
        return componentName;
    }

    public void setComponentName(@Nullable String componentName) {
        this.componentName = componentName;
    }

    public @Nullable Integer getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(@Nullable Integer componentVersion) {
        this.componentVersion = componentVersion;
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments;
    }

    public Status getStatus() {
        return Status.values()[status];
    }

    public void setStatus(Status status) {
        this.status = status.ordinal();
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
    }

    public @Nullable String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(@Nullable String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public @Nullable String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(@Nullable String llmModel) {
        this.llmModel = llmModel;
    }

    public @Nullable Long getDecidedByUserId() {
        return decidedByUserId;
    }

    public void setDecidedByUserId(@Nullable Long decidedByUserId) {
        this.decidedByUserId = decidedByUserId;
    }

    public @Nullable Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(@Nullable Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public @Nullable String getComment() {
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public @Nullable String getExecutionError() {
        return executionError;
    }

    public void setExecutionError(@Nullable String executionError) {
        this.executionError = executionError;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
