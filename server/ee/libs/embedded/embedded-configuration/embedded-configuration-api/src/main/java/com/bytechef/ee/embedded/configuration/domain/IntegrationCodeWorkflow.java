/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("integration_code_workflow")
public class IntegrationCodeWorkflow {

    @Column("code_workflow_container_id")
    private AggregateReference<CodeWorkflowContainer, Long> codeWorkflowContainerId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("integration_id")
    private AggregateReference<Integration, Long> integrationId;

    @Column("integration_version")
    private int integrationVersion;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IntegrationCodeWorkflow integrationCodeWorkflow)) {
            return false;
        }

        return Objects.equals(id, integrationCodeWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getCodeWorkflowContainerId() {
        return codeWorkflowContainerId.getId();
    }

    public Long getId() {
        return id;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getIntegrationId() {
        return integrationId.getId();
    }

    public int getIntegrationVersion() {
        return integrationVersion;
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

    public void setCodeWorkflowContainer(CodeWorkflowContainer codeWorkflowContainer) {
        this.codeWorkflowContainerId = AggregateReference.to(codeWorkflowContainer.getId());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setIntegration(Integration integration) {
        this.integrationId = AggregateReference.to(integration.getId());
    }

    public void setIntegrationVersion(int integrationVersion) {
        this.integrationVersion = integrationVersion;
    }

    @Override
    public String toString() {
        return "IntegrationCodeWorkflow{" +
            "id=" + id +
            ", integrationId=" + integrationId +
            ", integrationVersion=" + integrationVersion +
            ", codeWorkflowContainerId=" + codeWorkflowContainerId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
