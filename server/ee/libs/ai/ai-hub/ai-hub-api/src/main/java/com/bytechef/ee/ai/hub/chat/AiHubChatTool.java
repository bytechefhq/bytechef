/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Map;
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
 * One configured cluster-element invocation attached to a {@link AiHubChatComponent}. Carries the pre-set parameter
 * map; at LLM dispatch time these merge with the LLM-supplied JSON args (LLM args override). Mirrors {@code McpTool}
 * but scoped to a ai_hub_chat_component.
 *
 * <p>
 * Re-adding the same action to the same component binding upserts via the unique constraint
 * {@code uk_cct_component_name} — chat affordance "add the sendMessage tool with channel #engineering" is therefore
 * replay-safe and the parameters reflect the latest call.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_chat_tool")
public class AiHubChatTool {

    @Id
    private Long id;

    @Column("chat_component_id")
    private long chatComponentId;

    @Column
    private String name;

    /**
     * MapWrapper-converted JSONB so Spring Data JDBC's converter chain handles serialization. Same trick McpTool uses;
     * keeps the parameter shape arbitrary without a typed schema.
     */
    @Column
    private MapWrapper parameters;

    // Per-tool on/off for the Connectors page (img 25). Defaults to enabled.
    @Column("enabled")
    private boolean enabled = true;

    @CreatedBy
    @Column("created_by")
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

    public AiHubChatTool() {
    }

    public AiHubChatTool(long chatComponentId, String name, Map<String, ?> parameters) {
        this.chatComponentId = chatComponentId;
        this.name = name;
        this.parameters = new MapWrapper(parameters);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getChatComponentId() {
        return chatComponentId;
    }

    public void setChatComponentId(long chatComponentId) {
        this.chatComponentId = chatComponentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, ?> getParameters() {
        return parameters == null ? Map.of() : parameters.getMap();
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = new MapWrapper(parameters);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AiHubChatTool that)) {
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
        return "AiHubChatTool{" +
            "id=" + id +
            ", chatComponentId=" + chatComponentId +
            ", name='" + name + '\'' +
            ", parameters=" + parameters +
            ", version=" + version +
            '}';
    }
}
