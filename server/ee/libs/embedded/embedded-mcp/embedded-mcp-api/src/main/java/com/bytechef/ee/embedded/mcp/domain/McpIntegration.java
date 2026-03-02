/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.domain;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.platform.mcp.domain.McpServer;
import java.time.Instant;
import java.util.Objects;
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
 * Domain class representing an MCP integration.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table
public final class McpIntegration {

    @Id
    private Long id;

    @Column("mcp_server_id")
    private AggregateReference<McpServer, Long> mcpServerId;

    @Column("integration_instance_configuration_id")
    private AggregateReference<IntegrationInstanceConfiguration, Long> integrationInstanceConfigurationId;

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

    public McpIntegration() {
    }

    public McpIntegration(long id) {
        this.id = id;
    }

    public McpIntegration(long integrationInstanceConfigurationId, long mcpServerId) {
        this.integrationInstanceConfigurationId = AggregateReference.to(integrationInstanceConfigurationId);
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public McpIntegration(long id, long integrationInstanceConfigurationId, long mcpServerId) {
        this.id = id;
        this.integrationInstanceConfigurationId = AggregateReference.to(integrationInstanceConfigurationId);
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public Long getIntegrationInstanceConfigurationId() {
        return integrationInstanceConfigurationId != null ? integrationInstanceConfigurationId.getId() : null;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getMcpServerId() {
        return mcpServerId.getId();
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        McpIntegration mcpIntegration = (McpIntegration) object;

        return Objects.equals(id, mcpIntegration.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
        if (integrationInstanceConfigurationId != null) {
            this.integrationInstanceConfigurationId = AggregateReference.to(integrationInstanceConfigurationId);
        }
    }

    public void setMcpServerId(Long mcpServerId) {
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "McpIntegration{" +
            "id=" + id +
            ", integrationInstanceConfigurationId=" +
            (integrationInstanceConfigurationId != null ? integrationInstanceConfigurationId.getId() : null) +
            ", mcpServerId=" + mcpServerId +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
