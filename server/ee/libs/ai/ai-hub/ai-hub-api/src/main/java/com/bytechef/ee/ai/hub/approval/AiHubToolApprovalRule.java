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
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A per-workspace rule that either requires or exempts a catalog tool or component action from the approval gate.
 * {@link #toolKind} reuses {@link AiHubToolApproval.ToolKind} so the ordinal contract lives in one place — this class
 * declares no enum of its own for it. A null {@link #componentName} means the rule applies to every component; combined
 * with {@link #toolName} this is what the unique index in the init changelog collapses via
 * {@code COALESCE(component_name, '')}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_tool_approval_rule")
public final class AiHubToolApprovalRule {

    public enum Mode {
        REQUIRE, EXEMPT
    }

    @Id
    private Long id;

    @Column("workspace_id")
    private long workspaceId;

    @Column("tool_kind")
    private int toolKind;

    @Column("component_name")
    private @Nullable String componentName;

    @Column("tool_name")
    private String toolName;

    @Column
    private int mode;

    @Column("created_by")
    @CreatedBy
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public AiHubToolApprovalRule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public AiHubToolApproval.ToolKind getToolKind() {
        return AiHubToolApproval.ToolKind.values()[toolKind];
    }

    public void setToolKind(AiHubToolApproval.ToolKind toolKind) {
        this.toolKind = toolKind.ordinal();
    }

    public @Nullable String getComponentName() {
        return componentName;
    }

    public void setComponentName(@Nullable String componentName) {
        this.componentName = componentName;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Mode getMode() {
        return Mode.values()[mode];
    }

    public void setMode(Mode mode) {
        this.mode = mode.ordinal();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
