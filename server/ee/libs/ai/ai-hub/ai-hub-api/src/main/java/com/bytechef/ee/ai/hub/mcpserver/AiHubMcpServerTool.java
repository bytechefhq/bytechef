/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-tool on/off state for a registered MCP server's tools (the Connectors page "Custom MCP" per-tool toggles). A tool
 * with no row is enabled by default; toggling a tool off persists a row with {@code enabled = false} so the agent's MCP
 * tool bridge filters it out. The tool list itself is discovered live from the server — this table only records
 * deviations from the all-tools-enabled default.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_mcp_server_tool")
public class AiHubMcpServerTool {

    @Id
    private Long id;

    @Column("mcp_server_id")
    private long mcpServerId;

    @Column
    private String name;

    @Column("enabled")
    private boolean enabled = true;

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

    public AiHubMcpServerTool() {
    }

    public AiHubMcpServerTool(long mcpServerId, String name, boolean enabled) {
        this.mcpServerId = mcpServerId;
        this.name = name;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public long getMcpServerId() {
        return mcpServerId;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMcpServerId(long mcpServerId) {
        this.mcpServerId = mcpServerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "AiHubMcpServerTool{" +
            "id=" + id +
            ", mcpServerId=" + mcpServerId +
            ", name='" + name + '\'' +
            ", enabled=" + enabled +
            ", version=" + version +
            '}';
    }
}
