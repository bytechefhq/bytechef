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

package com.bytechef.automation.configuration.domain;

import com.bytechef.platform.configuration.domain.McpServer;
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
 * Domain class representing an MCP project.
 *
 * @author Ivica Cardic
 */
@Table
public final class McpProject {

    @Id
    private Long id;

    @Column("mcp_server_id")
    private AggregateReference<McpServer, Long> mcpServerId;

    @Column("project_deployment_id")
    private AggregateReference<ProjectDeployment, Long> projectDeploymentId;

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

    public McpProject() {
    }

    public McpProject(Long id) {
        this.id = id;
    }

    public McpProject(Long projectDeploymentId, Long mcpServerId) {
        this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public McpProject(Long id, Long projectDeploymentId, Long mcpServerId) {
        this.id = id;
        this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getProjectDeploymentId() {
        return projectDeploymentId != null ? projectDeploymentId.getId() : null;
    }

    public Long getMcpServerId() {
        return mcpServerId.getId();
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        McpProject mcpProject = (McpProject) o;

        return Objects.equals(id, mcpProject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public void setProjectDeploymentId(Long projectDeploymentId) {
        if (projectDeploymentId != null) {
            this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
        }
    }

    public void setMcpServerId(Long mcpServerId) {
        this.mcpServerId = AggregateReference.to(mcpServerId);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "McpProject{" +
            "id=" + id +
            ", projectDeploymentId=" + (projectDeploymentId != null ? projectDeploymentId.getId() : null) +
            ", mcpServerId=" + mcpServerId +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
