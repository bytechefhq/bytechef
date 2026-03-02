/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.domain;

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
 * Domain class representing an MCP integration workflow.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("mcp_integration_workflow")
public final class McpIntegrationWorkflow {

    @Id
    private Long id;

    @Column("mcp_integration_id")
    private Long mcpIntegrationId;

    @Column("integration_instance_configuration_workflow_id")
    private Long integrationInstanceConfigurationWorkflowId;

    @Column
    private MapWrapper parameters = new MapWrapper();

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

    public McpIntegrationWorkflow() {
    }

    public McpIntegrationWorkflow(long mcpIntegrationId, long integrationInstanceConfigurationWorkflowId) {
        this.mcpIntegrationId = mcpIntegrationId;
        this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMcpIntegrationId() {
        return mcpIntegrationId;
    }

    public void setMcpIntegrationId(Long mcpIntegrationId) {
        this.mcpIntegrationId = mcpIntegrationId;
    }

    public Long getIntegrationInstanceConfigurationWorkflowId() {
        return integrationInstanceConfigurationWorkflowId;
    }

    public void setIntegrationInstanceConfigurationWorkflowId(Long integrationInstanceConfigurationWorkflowId) {
        this.integrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId;
    }

    public Map<String, ?> getParameters() {
        return parameters.getMap();
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = new MapWrapper(parameters);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof McpIntegrationWorkflow mcpIntegrationWorkflow)) {
            return false;
        }

        return Objects.equals(id, mcpIntegrationWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "McpIntegrationWorkflow{" +
            "id=" + id +
            ", mcpIntegrationId=" + mcpIntegrationId +
            ", integrationInstanceConfigurationWorkflowId=" + integrationInstanceConfigurationWorkflowId +
            ", parameters=" + parameters +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
