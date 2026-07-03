/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-agent tool template. One row = one (componentName, componentVersion, operationName) tuple the agent pre-declares
 * as available, optionally pinned to a specific connection and pre-set parameters. When a task is created against this
 * agent, the routing layer copies these rows into {@code ai_hub_task_tool} so the LLM sees the tools attached on the
 * very first turn — same UX as the AI Agent simple-mode tools picker, where an agent declares its capabilities once and
 * every chat with that agent starts with those tools available.
 *
 * <p>
 * <b>Per-tool config (added in 20260506000001):</b> {@code connectionId} pins the tool to a specific connection so
 * every spawned task runs against the same authenticated workspace ("the sendMessage tool with my Slack workspace
 * #engineering"); {@code parameters} pre-seeds the LLM-invocation argument map ("with channel #release-notes"). Both
 * are optional — null connectionId means "let the user pick at first invocation" and an empty parameters map means
 * "every argument is LLM-supplied". The task copy path forwards both verbatim into {@code ai_hub_task_tool}.
 * </p>
 *
 * <p>
 * <b>Lifecycle:</b> rows live as long as their parent {@link AiHubPersonalAgent}. Cascade delete on the FK ensures that
 * removing an agent cleans up its tool template; existing tasks spawned from the agent keep their copied tools (those
 * live in {@code ai_hub_task_tool} and aren't FK-linked to this table). Deleting a connection sets {@code connectionId}
 * to NULL on referencing rows (FK with {@code ON DELETE SET NULL}) — the agent's "I want to use Slack" intent stays
 * valid; the user just needs to pick a fresh connection on next configure.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_personal_agent_tool")
public class AiHubPersonalAgentTool {

    @Id
    private Long id;

    @Column("ai_hub_personal_agent_id")
    private long aiHubPersonalAgentId;

    @Column("component_name")
    private String componentName;

    @Column("component_version")
    private int componentVersion;

    @Column("operation_name")
    private String operationName;

    /**
     * Optional pinned connection. Nullable on the schema side via {@code ON DELETE SET NULL}: a connection deletion
     * after the tool was configured doesn't drop the tool entry, just clears the binding so the task copy path can fall
     * back to "no pre-set connection" (same as a tool that was never configured).
     */
    @Column("connection_id")
    @Nullable
    private Long connectionId;

    /**
     * Pre-set parameters map. Same MapWrapper-backed JSONB approach as {@code AiHubTaskTool.parameters} so the task
     * copy path is a straight assignment rather than a shape transformation. Defaults to empty map at the schema level
     * (DDL: {@code defaultValueComputed="'{}'::jsonb"}); the entity side mirrors that with a null check + empty-map
     * fallback in the getter.
     */
    @Column
    @Nullable
    private MapWrapper parameters;

    @Column("created_at")
    private LocalDateTime createdAt;

    public AiHubPersonalAgentTool() {
    }

    public AiHubPersonalAgentTool(long aiHubPersonalAgentId, String componentName, int componentVersion,
        String operationName) {
        this.aiHubPersonalAgentId = aiHubPersonalAgentId;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.operationName = operationName;
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

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    @Nullable
    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * Returns the pre-set parameters map. Falls back to an empty map for newly-loaded rows whose JSONB column was
     * stored as the default {@code '{}'} — the underlying {@link MapWrapper} can be null when the row was inserted
     * before this column existed and never re-saved (defensive against schema-evolution edge cases).
     */
    public Map<String, ?> getParameters() {
        return parameters == null ? Map.of() : parameters.getMap();
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = new MapWrapper(parameters);
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

        if (!(o instanceof AiHubPersonalAgentTool that)) {
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
        return "AiHubPersonalAgentTool{" +
            "id=" + id +
            ", aiHubPersonalAgentId=" + aiHubPersonalAgentId +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", operationName='" + operationName + '\'' +
            ", connectionId=" + connectionId +
            '}';
    }
}
