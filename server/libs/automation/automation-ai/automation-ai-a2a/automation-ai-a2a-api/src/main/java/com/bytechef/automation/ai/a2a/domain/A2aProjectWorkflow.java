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

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.time.Instant;
import java.util.Map;
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
 * Domain class linking an {@link A2aProject} to a project deployment workflow that is exposed as an A2A skill.
 *
 * <p>
 * The A2A skill metadata ({@code skillName}, {@code skillDescription}, {@code skillTags}) lives inside
 * {@link #parameters} rather than in dedicated columns, mirroring how {@code McpProjectWorkflow} keeps its tool mapping
 * in a serialized parameter map.
 * </p>
 *
 * @author Ivica Cardic
 */
@Table("a2a_project_workflow")
public final class A2aProjectWorkflow {

    public static final String SKILL_NAME = "skillName";
    public static final String SKILL_DESCRIPTION = "skillDescription";
    public static final String SKILL_TAGS = "skillTags";

    @Id
    private Long id;

    @Column("a2a_project_id")
    private Long a2aProjectId;

    @Column("project_deployment_workflow_id")
    private Long projectDeploymentWorkflowId;

    @Column
    private MapWrapper parameters = new MapWrapper();

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

    public A2aProjectWorkflow() {
    }

    public A2aProjectWorkflow(long a2aProjectId, long projectDeploymentWorkflowId) {
        this.a2aProjectId = a2aProjectId;
        this.projectDeploymentWorkflowId = projectDeploymentWorkflowId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        A2aProjectWorkflow a2aProjectWorkflow = (A2aProjectWorkflow) o;

        return Objects.equals(id, a2aProjectWorkflow.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getA2aProjectId() {
        return a2aProjectId;
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

    public Map<String, ?> getParameters() {
        return parameters.getMap();
    }

    public Long getProjectDeploymentWorkflowId() {
        return projectDeploymentWorkflowId;
    }

    public int getVersion() {
        return version;
    }

    public void setA2aProjectId(Long a2aProjectId) {
        this.a2aProjectId = a2aProjectId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = new MapWrapper(parameters);
    }

    public void setProjectDeploymentWorkflowId(Long projectDeploymentWorkflowId) {
        this.projectDeploymentWorkflowId = projectDeploymentWorkflowId;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "A2aProjectWorkflow{" +
            "id=" + id +
            ", a2aProjectId=" + a2aProjectId +
            ", projectDeploymentWorkflowId=" + projectDeploymentWorkflowId +
            ", parameters=" + parameters +
            ", version=" + version +
            '}';
    }
}
