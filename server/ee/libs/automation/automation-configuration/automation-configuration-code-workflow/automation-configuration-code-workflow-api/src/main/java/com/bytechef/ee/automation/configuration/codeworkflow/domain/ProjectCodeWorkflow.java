/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.codeworkflow.domain;

import com.bytechef.automation.configuration.domain.Project;
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
@Table("project_code_workflow")
public class ProjectCodeWorkflow {

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

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @Column("project_version")
    private int projectVersion;

    @Version
    private int version;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProjectCodeWorkflow projectCodeWorkflow)) {
            return false;
        }

        return Objects.equals(id, projectCodeWorkflow.id);
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

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getProjectId() {
        return projectId.getId();
    }

    public int getProjectVersion() {
        return projectVersion;
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

    public void setProject(Project project) {
        this.projectId = AggregateReference.to(project.getId());
    }

    public void setProjectVersion(int projectVersion) {
        this.projectVersion = projectVersion;
    }

    @Override
    public String toString() {
        return "ProjectCodeWorkflow{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", projectVersion=" + projectVersion +
            ", codeWorkflowContainerId=" + codeWorkflowContainerId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
