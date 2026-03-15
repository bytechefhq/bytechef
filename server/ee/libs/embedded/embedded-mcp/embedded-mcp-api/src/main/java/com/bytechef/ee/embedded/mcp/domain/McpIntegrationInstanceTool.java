/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.domain;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.platform.mcp.domain.McpTool;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("mcp_integration_instance_tool")
public final class McpIntegrationInstanceTool {

    @Id
    private Long id;

    @Column("integration_instance_id")
    private AggregateReference<IntegrationInstance, Long> integrationInstanceId;

    @Column("mcp_tool_id")
    private AggregateReference<McpTool, Long> mcpToolId;

    @Column
    private boolean enabled;

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

    public McpIntegrationInstanceTool() {
    }

    public McpIntegrationInstanceTool(long integrationInstanceId, long mcpToolId, boolean enabled) {
        this.integrationInstanceId = AggregateReference.to(integrationInstanceId);
        this.mcpToolId = AggregateReference.to(mcpToolId);
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public Long getIntegrationInstanceId() {
        return Validate.notNull(integrationInstanceId, "integrationInstanceId")
            .getId();
    }

    public Long getMcpToolId() {
        return Validate.notNull(mcpToolId, "mcpToolId")
            .getId();
    }

    public boolean isEnabled() {
        return enabled;
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

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationInstanceId(long integrationInstanceId) {
        this.integrationInstanceId = AggregateReference.to(integrationInstanceId);
    }

    public void setMcpToolId(long mcpToolId) {
        this.mcpToolId = AggregateReference.to(mcpToolId);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof McpIntegrationInstanceTool mcpIntegrationInstanceTool)) {
            return false;
        }

        return Objects.equals(id, mcpIntegrationInstanceTool.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "McpIntegrationInstanceTool{" +
            "id=" + id +
            ", integrationInstanceId=" + integrationInstanceId +
            ", mcpToolId=" + mcpToolId +
            ", enabled=" + enabled +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
