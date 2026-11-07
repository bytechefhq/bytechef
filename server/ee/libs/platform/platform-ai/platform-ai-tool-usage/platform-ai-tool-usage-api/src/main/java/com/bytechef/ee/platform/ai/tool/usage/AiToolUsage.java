/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_tool_usage")
public class AiToolUsage {

    @Id
    private Long id;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    @Column("user_id")
    private long userId;

    @Column("owner_id")
    private Long ownerId;

    @Column("tool_name")
    private String toolName;

    @Column("unit_count")
    private int unitCount;

    @Column("estimated_cost_usd")
    private BigDecimal estimatedCostUsd;

    @Column("duration_ms")
    private int durationMs;

    @Column("metadata_json")
    private String metadataJson;

    @Column("environment")
    private int environment;

    @Column("created_at")
    private LocalDateTime createdAt;

    AiToolUsage() {
    }

    public Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public long getUserId() {
        return userId;
    }

    void setUserId(long userId) {
        this.userId = userId;
    }

    public @Nullable Long getOwnerId() {
        return ownerId;
    }

    void setOwnerId(@Nullable Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getToolName() {
        return toolName;
    }

    void setToolName(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("AiToolUsage.toolName must not be null or blank");
        }

        this.toolName = toolName;
    }

    public int getUnitCount() {
        return unitCount;
    }

    void setUnitCount(int unitCount) {
        if (unitCount < 0) {
            throw new IllegalArgumentException("AiToolUsage.unitCount must be >= 0 (got " + unitCount + ")");
        }

        this.unitCount = unitCount;
    }

    public BigDecimal getEstimatedCostUsd() {
        return estimatedCostUsd;
    }

    void setEstimatedCostUsd(BigDecimal estimatedCostUsd) {
        if (estimatedCostUsd == null) {
            throw new IllegalArgumentException("AiToolUsage.estimatedCostUsd must not be null");
        }

        if (estimatedCostUsd.signum() < 0) {
            throw new IllegalArgumentException(
                "AiToolUsage.estimatedCostUsd must be >= 0 (got " + estimatedCostUsd + ")");
        }

        this.estimatedCostUsd = estimatedCostUsd;
    }

    public int getDurationMs() {
        return durationMs;
    }

    void setDurationMs(int durationMs) {
        if (durationMs < 0) {
            throw new IllegalArgumentException("AiToolUsage.durationMs must be >= 0 (got " + durationMs + ")");
        }

        this.durationMs = durationMs;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public int getEnvironment() {
        return environment;
    }

    void setEnvironment(int environment) {
        this.environment = environment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiToolUsage that = (AiToolUsage) other;

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
        return "AiToolUsage{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", userId=" + userId +
            ", ownerId=" + ownerId +
            ", toolName='" + toolName + '\'' +
            ", unitCount=" + unitCount +
            ", estimatedCostUsd=" + estimatedCostUsd +
            ", durationMs=" + durationMs +
            ", environment=" + environment +
            ", createdAt=" + createdAt +
            '}';
    }
}
