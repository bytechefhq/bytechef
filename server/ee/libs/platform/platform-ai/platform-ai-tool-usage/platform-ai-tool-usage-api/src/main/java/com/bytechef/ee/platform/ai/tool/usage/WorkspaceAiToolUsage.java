/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

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
 * Workspace ↔ {@link AiToolUsage} membership row. Insert-path mandatory; shares the parent transaction so a half-write
 * doesn't orphan the usage row from cost dashboards.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workspace_ai_tool_usage")
public class WorkspaceAiToolUsage {

    @Id
    private Long id;

    @Column("workspace_id")
    private Long workspaceId;

    @Column("ai_tool_usage_id")
    private Long aiToolUsageId;

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

    public WorkspaceAiToolUsage() {
    }

    public WorkspaceAiToolUsage(Long workspaceId, Long aiToolUsageId) {
        this.workspaceId = workspaceId;
        this.aiToolUsageId = aiToolUsageId;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public Long getAiToolUsageId() {
        return aiToolUsageId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        WorkspaceAiToolUsage that = (WorkspaceAiToolUsage) other;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "WorkspaceAiToolUsage{id=" + id + ", workspaceId=" + workspaceId
            + ", aiToolUsageId=" + aiToolUsageId + '}';
    }
}
