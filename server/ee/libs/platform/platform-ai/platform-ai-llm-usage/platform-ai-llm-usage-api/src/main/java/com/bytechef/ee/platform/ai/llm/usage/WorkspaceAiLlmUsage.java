/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Workspace ↔ {@link AiLlmUsage} membership row. Mirrors the workspace_mcp_server / workspace_ai_hub_* pattern. The
 * membership insert is on the hot path of every recorded LLM call, so it must share the same transaction as the parent
 * insert; an orphaned ai_llm_usage row would silently disappear from cost dashboards (which JOIN through this table for
 * the workspace dimension).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workspace_ai_llm_usage")
public class WorkspaceAiLlmUsage {

    @Id
    private Long id;

    @Column("workspace_id")
    private Long workspaceId;

    @Column("ai_llm_usage_id")
    private Long aiLlmUsageId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    @Version
    private int version;

    public WorkspaceAiLlmUsage() {
    }

    public WorkspaceAiLlmUsage(Long workspaceId, Long aiLlmUsageId) {
        this.workspaceId = workspaceId;
        this.aiLlmUsageId = aiLlmUsageId;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getAiLlmUsageId() {
        return aiLlmUsageId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        WorkspaceAiLlmUsage that = (WorkspaceAiLlmUsage) other;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkspaceAiLlmUsage{id=" + id + ", workspaceId=" + workspaceId
            + ", aiLlmUsageId=" + aiLlmUsageId + '}';
    }
}
