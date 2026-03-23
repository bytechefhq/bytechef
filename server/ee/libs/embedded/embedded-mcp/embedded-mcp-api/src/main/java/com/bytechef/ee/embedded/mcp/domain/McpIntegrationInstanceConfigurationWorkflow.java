/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import java.time.Instant;
import java.util.Map;
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
@Table("mcp_integration_instance_configuration_workflow")
public final class McpIntegrationInstanceConfigurationWorkflow {

    @Id
    private Long id;

    @Column("mcp_integration_instance_configuration_id")
    private AggregateReference<McpIntegrationInstanceConfiguration, Long> mcpIntegrationInstanceConfigurationId;

    @Column("integration_instance_configuration_workflow_id")
    private AggregateReference<IntegrationInstanceConfigurationWorkflow, Long> integrationInstanceConfigurationWorkflowId;

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

    public McpIntegrationInstanceConfigurationWorkflow() {
    }

    public McpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationId,
        long integrationInstanceConfigurationWorkflowId) {
        this.mcpIntegrationInstanceConfigurationId = AggregateReference.to(mcpIntegrationInstanceConfigurationId);
        this.integrationInstanceConfigurationWorkflowId = AggregateReference.to(
            integrationInstanceConfigurationWorkflowId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMcpIntegrationInstanceConfigurationId() {
        return Validate.notNull(mcpIntegrationInstanceConfigurationId, "mcpIntegrationInstanceConfigurationId")
            .getId();
    }

    public void setMcpIntegrationInstanceConfigurationId(long mcpIntegrationInstanceConfigurationId) {
        this.mcpIntegrationInstanceConfigurationId = AggregateReference.to(mcpIntegrationInstanceConfigurationId);
    }

    public Long getIntegrationInstanceConfigurationWorkflowId() {
        return Validate.notNull(
            integrationInstanceConfigurationWorkflowId, "integrationInstanceConfigurationWorkflowId")
            .getId();
    }

    public void setIntegrationInstanceConfigurationWorkflowId(long integrationInstanceConfigurationWorkflowId) {
        this.integrationInstanceConfigurationWorkflowId = AggregateReference.to(
            integrationInstanceConfigurationWorkflowId);
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
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow)) {
            return false;
        }

        return Objects.equals(id, mcpIntegrationInstanceConfigurationWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "McpIntegrationInstanceConfigurationWorkflow{" +
            "id=" + id +
            ", mcpIntegrationInstanceConfigurationId=" + mcpIntegrationInstanceConfigurationId +
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
