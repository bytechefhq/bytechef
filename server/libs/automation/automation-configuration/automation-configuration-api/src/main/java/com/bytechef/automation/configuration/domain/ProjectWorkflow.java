/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.domain;

import java.time.Instant;
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
 * @author Ivica Cardic
 */
@Table("project_workflow")
public final class ProjectWorkflow {

    @Id
    private Long id;

    @Column("project_id")
    private long projectId;

    @Column("project_version")
    private int projectVersion;

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
    private Instant lastModifiedDate;

    @Version
    private int version;

    public ProjectWorkflow() {
    }

    public ProjectWorkflow(long id) {
        this.id = id;
    }

    public ProjectWorkflow(long projectId, int projectVersion, String workflowId, String workflowReferenceCode) {
        this.projectId = projectId;
        this.projectVersion = projectVersion;
        this.workflowId = workflowId;
        this.workflowReferenceCode = workflowReferenceCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectWorkflow projectWorkflow = (ProjectWorkflow) o;

        return Objects.equals(id, projectWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getId() {
        return id;
    }

    public long getProjectId() {
        return projectId;
    }

    public int getProjectVersion() {
        return projectVersion;
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

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setProjectVersion(int projectVersion) {
        this.projectVersion = projectVersion;
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
        return "ProjectWorkflow{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", projectVersion=" + projectVersion +
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
