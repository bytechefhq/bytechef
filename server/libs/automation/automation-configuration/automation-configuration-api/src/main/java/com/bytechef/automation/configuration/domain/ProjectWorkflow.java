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

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@Table("project_workflow")
public final class ProjectWorkflow {

    @Column("project_version")
    private int projectVersion;

    @Column("workflow_id")
    private String workflowId;

    public ProjectWorkflow() {
    }

    public ProjectWorkflow(String workflowId, int projectVersion) {
        this.workflowId = workflowId;
        this.projectVersion = projectVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProjectWorkflow that)) {
            return false;
        }

        return projectVersion == that.projectVersion && Objects.equals(workflowId, that.workflowId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectVersion, workflowId);
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public int getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(int projectVersion) {
        this.projectVersion = projectVersion;
    }

    @Override
    public String toString() {
        return "ProjectWorkflow{" +
            "projectVersion=" + projectVersion +
            ", workflowId='" + workflowId + '\'' +
            '}';
    }
}
