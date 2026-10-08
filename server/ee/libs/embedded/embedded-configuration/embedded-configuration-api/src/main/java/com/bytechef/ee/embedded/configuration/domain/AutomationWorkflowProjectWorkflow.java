/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import com.bytechef.automation.configuration.domain.Project;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
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
@Table("automation_workflow_project_workflow")
public class AutomationWorkflowProjectWorkflow {

    @Id
    private Long id;

    @Column("permission_expression")
    private String permissionExpression;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @Column("workflow_uuid")
    private UUID workflowUuid;

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

    public AutomationWorkflowProjectWorkflow() {
    }

    public AutomationWorkflowProjectWorkflow(long projectId, UUID workflowUuid) {
        this.projectId = AggregateReference.to(projectId);
        this.workflowUuid = workflowUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AutomationWorkflowProjectWorkflow that)) {
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

    public String getPermissionExpression() {
        return permissionExpression;
    }

    public Long getProjectId() {
        return projectId.getId();
    }

    public int getVersion() {
        return version;
    }

    public UUID getWorkflowUuid() {
        return workflowUuid;
    }

    public void setPermissionExpression(String permissionExpression) {
        this.permissionExpression = permissionExpression;
    }

    @Override
    public String toString() {
        return "AutomationWorkflowProjectWorkflow{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", workflowUuid=" + workflowUuid +
            ", permissionExpression='" + permissionExpression + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
