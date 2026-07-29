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

package com.bytechef.automation.ai.a2a.domain;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
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
 * Domain class linking an {@link A2aServer} to a project deployment whose workflows are exposed over A2A.
 *
 * @author Ivica Cardic
 */
@Table("a2a_project")
public final class A2aProject {

    @Id
    private Long id;

    @Column("a2a_server_id")
    private AggregateReference<A2aServer, Long> a2aServerId;

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

    public A2aProject() {
    }

    public A2aProject(long id) {
        this.id = id;
    }

    public A2aProject(long projectDeploymentId, long a2aServerId) {
        this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
        this.a2aServerId = AggregateReference.to(a2aServerId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        A2aProject a2aProject = (A2aProject) o;

        return Objects.equals(id, a2aProject.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getA2aServerId() {
        return a2aServerId != null ? a2aServerId.getId() : null;
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

    public int getVersion() {
        return version;
    }

    public void setA2aServerId(Long a2aServerId) {
        if (a2aServerId != null) {
            this.a2aServerId = AggregateReference.to(a2aServerId);
        }
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setProjectDeploymentId(Long projectDeploymentId) {
        if (projectDeploymentId != null) {
            this.projectDeploymentId = AggregateReference.to(projectDeploymentId);
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "A2aProject{" +
            "id=" + id +
            ", a2aServerId=" + getA2aServerId() +
            ", projectDeploymentId=" + getProjectDeploymentId() +
            ", version=" + version +
            '}';
    }
}
