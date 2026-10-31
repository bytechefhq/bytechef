/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.domain;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.platform.mcp.domain.McpComponent;
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
 * Binds an MCP component of an embedded MCP server to the integration whose instances supply its connection and tool
 * toggles.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("mcp_integration_component")
public final class McpIntegrationComponent {

    @Id
    private Long id;

    @Column("mcp_component_id")
    private AggregateReference<McpComponent, Long> mcpComponentId;

    @Column("integration_id")
    private AggregateReference<Integration, Long> integrationId;

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

    public McpIntegrationComponent() {
    }

    public McpIntegrationComponent(long mcpComponentId, long integrationId) {
        this.mcpComponentId = AggregateReference.to(mcpComponentId);
        this.integrationId = AggregateReference.to(integrationId);
    }

    public Long getId() {
        return id;
    }

    public Long getMcpComponentId() {
        return Validate.notNull(mcpComponentId, "mcpComponentId")
            .getId();
    }

    public Long getIntegrationId() {
        return Validate.notNull(integrationId, "integrationId")
            .getId();
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

    public void setMcpComponentId(long mcpComponentId) {
        this.mcpComponentId = AggregateReference.to(mcpComponentId);
    }

    public void setIntegrationId(long integrationId) {
        this.integrationId = AggregateReference.to(integrationId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof McpIntegrationComponent mcpIntegrationComponent)) {
            return false;
        }

        return Objects.equals(id, mcpIntegrationComponent.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "McpIntegrationComponent{" +
            "id=" + id +
            ", mcpComponentId=" + mcpComponentId +
            ", integrationId=" + integrationId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
