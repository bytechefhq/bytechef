/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-agent resource template. One row = one (kind, resourceId, resourceName) reference the agent pre-declares — a
 * workflow, file, data table, knowledge base, MCP server, API collection, workflow execution, or previous task. When a
 * task is created against this agent, the routing layer copies these rows into {@code ai_hub_task_artifact} as
 * {@code *_REFERENCED} artifacts so the resources surface in the spawned task — mirroring how
 * {@link AiHubPersonalAgentTool} rows are copied into {@code ai_hub_task_tool}.
 *
 * <p>
 * {@code resourceId} is VARCHAR rather than BIGINT: composer resource ids are strings and workflow ids are non-numeric,
 * so uniform string storage avoids per-kind columns.
 * </p>
 *
 * <p>
 * <b>Lifecycle:</b> rows live as long as their parent {@link AiHubPersonalAgent}; the FK cascades on agent delete.
 * Tasks already spawned from the agent keep their copied artifact rows (those live in {@code ai_hub_task_artifact} and
 * are not FK-linked to this table).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_personal_agent_resource")
public class AiHubPersonalAgentResource {

    @Id
    private Long id;

    @Column("ai_hub_personal_agent_id")
    private long aiHubPersonalAgentId;

    @Column("kind")
    private int kind;

    @Column("resource_id")
    private String resourceId;

    @Column("resource_name")
    private String resourceName;

    @Column("created_at")
    private LocalDateTime createdAt;

    public AiHubPersonalAgentResource() {
    }

    public AiHubPersonalAgentResource(
        long aiHubPersonalAgentId, AiHubPersonalAgentResourceKind kind, String resourceId, String resourceName) {

        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
        this.kind = kind.ordinal();
        this.resourceId = resourceId;
        this.resourceName = resourceName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAiHubPersonalAgentId() {
        return aiHubPersonalAgentId;
    }

    public void setAiHubPersonalAgentId(long aiHubPersonalAgentId) {
        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
    }

    public AiHubPersonalAgentResourceKind getKind() {
        return EnumOrdinals.fromOrdinal(kind, AiHubPersonalAgentResourceKind.class);
    }

    public void setKind(AiHubPersonalAgentResourceKind kind) {
        this.kind = kind.ordinal();
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AiHubPersonalAgentResource that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiHubPersonalAgentResource{" +
            "id=" + id +
            ", aiHubPersonalAgentId=" + aiHubPersonalAgentId +
            ", kind=" + kind +
            ", resourceId='" + resourceId + '\'' +
            ", resourceName='" + resourceName + '\'' +
            '}';
    }
}
