/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.domain;

import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.platform.mcp.domain.McpServer;
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
@Table("mcp_automation_project_configuration")
public final class McpAutomationProjectConfiguration {

    @Id
    private Long id;

    @Column("mcp_server_id")
    private AggregateReference<McpServer, Long> mcpServerId;

    @Column("automation_project_configuration_id")
    private AggregateReference<AutomationProjectConfiguration, Long> automationProjectConfigurationId;

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

    public McpAutomationProjectConfiguration() {
    }

    public McpAutomationProjectConfiguration(long automationProjectConfigurationId, long mcpServerId) {
        this.automationProjectConfigurationId = AggregateReference.to(automationProjectConfigurationId);
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAutomationProjectConfigurationId() {
        return Validate.notNull(automationProjectConfigurationId, "automationProjectConfigurationId")
            .getId();
    }

    public Long getMcpServerId() {
        return Validate.notNull(mcpServerId, "mcpServerId")
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

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof McpAutomationProjectConfiguration mcpAutomationProjectConfiguration)) {
            return false;
        }

        return Objects.equals(id, mcpAutomationProjectConfiguration.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "McpAutomationProjectConfiguration{" +
            "id=" + id +
            ", mcpServerId=" + mcpServerId +
            ", automationProjectConfigurationId=" + automationProjectConfigurationId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
