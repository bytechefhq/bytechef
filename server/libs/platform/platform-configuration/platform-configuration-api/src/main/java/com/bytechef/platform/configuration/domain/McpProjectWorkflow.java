/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.configuration.domain;

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
 * Domain class representing an MCP project workflow.
 *
 * @author Ivica Cardic
 */
@Table("mcp_project_workflow")
public final class McpProjectWorkflow {

    @Id
    private Long id;

    @Column("mcp_project_id")
    private Long mcpProjectId;

    @Column("project_deployment_workflow_id")
    private Long projectDeploymentWorkflowId;

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

    public McpProjectWorkflow() {
    }

    public McpProjectWorkflow(Long mcpProjectId, Long projectDeploymentWorkflowId) {
        this.mcpProjectId = mcpProjectId;
        this.projectDeploymentWorkflowId = projectDeploymentWorkflowId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMcpProjectId() {
        return mcpProjectId;
    }

    public void setMcpProjectId(Long mcpProjectId) {
        this.mcpProjectId = mcpProjectId;
    }

    public Long getProjectDeploymentWorkflowId() {
        return projectDeploymentWorkflowId;
    }

    public void setProjectDeploymentWorkflowId(Long projectDeploymentWorkflowId) {
        this.projectDeploymentWorkflowId = projectDeploymentWorkflowId;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof McpProjectWorkflow mcpProjectWorkflow)) {
            return false;
        }

        return Objects.equals(id, mcpProjectWorkflow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "McpProjectWorkflow{" +
            "id=" + id +
            ", mcpProjectId=" + mcpProjectId +
            ", projectDeploymentWorkflowId=" + projectDeploymentWorkflowId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}
