/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import java.time.Instant;
import java.time.LocalDateTime;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("integration_workflow")
public final class IntegrationWorkflow {

    @Id
    private Long id;

    @Column("integration_id")
    private long integrationId;

    @Column("integration_version")
    private int integrationVersion;

    @Column("workflow_id")
    private String workflowId;

    @Column("workflow_reference_code")
    private String workflowReferenceCode;

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
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public IntegrationWorkflow() {
    }

    public IntegrationWorkflow(long integrationId, int integrationVersion, String workflowId,
        String workflowReferenceCode) {
        this.integrationId = integrationId;
        this.integrationVersion = integrationVersion;
        this.workflowId = workflowId;
        this.workflowReferenceCode = workflowReferenceCode;
    }

    public IntegrationWorkflow(long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntegrationWorkflow integrationWorkflow = (IntegrationWorkflow) o;

        return Objects.equals(id, integrationWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public long getIntegrationId() {
        return integrationId;
    }

    public int getIntegrationVersion() {
        return integrationVersion;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowReferenceCode() {
        return workflowReferenceCode;
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

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setIntegrationVersion(int integrationVersion) {
        this.integrationVersion = integrationVersion;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public void setWorkflowReferenceCode(String workflowReferenceCode) {
        this.workflowReferenceCode = workflowReferenceCode;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "IntegrationWorkflow{" +
            "id=" + id +
            ", integrationId=" + integrationId +
            ", integrationVersion=" + integrationVersion +
            ", workflowId='" + workflowId + '\'' +
            ", workflowReferenceCode='" + workflowReferenceCode + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
