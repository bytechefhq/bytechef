/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity that connects ConnectedUserProject and Workflow.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("connected_user_project_workflow")
public class ConnectedUserProjectWorkflow {

    @Id
    private Long id;

    @Column("connected_user_project_id")
    private AggregateReference<ConnectedUserProject, Long> connectedUserProjectId;

    @Column("project_workflow_id")
    private AggregateReference<ProjectWorkflow, Long> projectWorkflowId;

    @Column("workflow_version")
    private Integer workflowVersion;

    @Column("catalog_workflow_uuid")
    private String catalogWorkflowUuid;

    @Column("copied_from_workflow_uuid")
    private String copiedFromWorkflowUuid;

    @Column("project_deployment_id")
    private Long projectDeploymentId;

    @Column
    private boolean enabled = true;

    @Column
    private boolean dangling;

    @Column("dangling_reason")
    private String danglingReason;

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

    public ConnectedUserProjectWorkflow() {
    }

    @PersistenceCreator
    public ConnectedUserProjectWorkflow(
        Long id, Long connectedUserProjectId, @Nullable Long projectWorkflowId, Integer workflowVersion,
        @Nullable String catalogWorkflowUuid, @Nullable Long projectDeploymentId, boolean enabled, boolean dangling,
        @Nullable String danglingReason, int version) {

        this.id = id;
        this.connectedUserProjectId = AggregateReference.to(connectedUserProjectId);
        this.projectWorkflowId = projectWorkflowId == null ? null : AggregateReference.to(projectWorkflowId);
        this.workflowVersion = workflowVersion;
        this.catalogWorkflowUuid = catalogWorkflowUuid;
        this.projectDeploymentId = projectDeploymentId;
        this.enabled = enabled;
        this.dangling = dangling;
        this.danglingReason = danglingReason;
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConnectedUserProjectWorkflow that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public Long getConnectedUserProjectId() {
        return connectedUserProjectId.getId();
    }

    @Nullable
    public Long getProjectWorkflowId() {
        return projectWorkflowId == null ? null : projectWorkflowId.getId();
    }

    @Nullable
    public String getCatalogWorkflowUuid() {
        return catalogWorkflowUuid;
    }

    public void setCatalogWorkflowUuid(@Nullable String catalogWorkflowUuid) {
        this.catalogWorkflowUuid = catalogWorkflowUuid;
    }

    /**
     * Set only on a copy-mode row provisioned implicitly by a sync {@code POST /workflows/{workflowUuid}} call against
     * a visual template's catalog uuid; records that catalog uuid so a repeated call against the same template resolves
     * this row instead of provisioning a second copy. Null for copy-mode rows created through any other path
     * (blank-workflow creation, prompt generation, the explicit copy endpoint) and for every reference-mode row.
     */
    @Nullable
    public String getCopiedFromWorkflowUuid() {
        return copiedFromWorkflowUuid;
    }

    public void setCopiedFromWorkflowUuid(@Nullable String copiedFromWorkflowUuid) {
        this.copiedFromWorkflowUuid = copiedFromWorkflowUuid;
    }

    @Nullable
    public Long getProjectDeploymentId() {
        return projectDeploymentId;
    }

    public void setProjectDeploymentId(@Nullable Long projectDeploymentId) {
        this.projectDeploymentId = projectDeploymentId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDangling() {
        return dangling;
    }

    public void setDangling(boolean dangling) {
        this.dangling = dangling;
    }

    @Nullable
    public String getDanglingReason() {
        return danglingReason;
    }

    public void setDanglingReason(@Nullable String danglingReason) {
        this.danglingReason = danglingReason;
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

    public Integer getWorkflowVersion() {
        return workflowVersion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConnectedUserProjectId(Long connectedUserProjectId) {
        this.connectedUserProjectId = AggregateReference.to(connectedUserProjectId);
    }

    public void setProjectWorkflowId(@Nullable Long projectWorkflowId) {
        this.projectWorkflowId = projectWorkflowId == null ? null : AggregateReference.to(projectWorkflowId);
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkflowVersion(Integer workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    @Override
    public String toString() {
        return "ConnectedUserProjectWorkflow{" +
            "id=" + id +
            ", connectedUserProjectId=" + connectedUserProjectId +
            ", projectWorkflowId=" + projectWorkflowId +
            ", workflowVersion=" + workflowVersion +
            ", catalogWorkflowUuid='" + catalogWorkflowUuid + '\'' +
            ", copiedFromWorkflowUuid='" + copiedFromWorkflowUuid + '\'' +
            ", projectDeploymentId=" + projectDeploymentId +
            ", enabled=" + enabled +
            ", dangling=" + dangling +
            ", danglingReason='" + danglingReason + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
