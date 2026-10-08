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
@Table("automation_workflow_project")
public class AutomationWorkflowProject {

    @Id
    private Long id;

    @Column("automation_hub_visible")
    private boolean automationHubVisible = true;

    @Column("permission_expression")
    private String permissionExpression;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

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

    public AutomationWorkflowProject() {
    }

    public AutomationWorkflowProject(long projectId) {
        this.projectId = AggregateReference.to(projectId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AutomationWorkflowProject that)) {
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

    public boolean isAutomationHubVisible() {
        return automationHubVisible;
    }

    public void setAutomationHubVisible(boolean automationHubVisible) {
        this.automationHubVisible = automationHubVisible;
    }

    public void setPermissionExpression(String permissionExpression) {
        this.permissionExpression = permissionExpression;
    }

    @Override
    public String toString() {
        return "AutomationWorkflowProject{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", automationHubVisible=" + automationHubVisible +
            ", permissionExpression='" + permissionExpression + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
