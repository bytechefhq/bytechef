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

package com.bytechef.automation.configuration.dto;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectDeploymentDTO(
    String createdBy, Instant createdDate, String description, boolean enabled, Environment environment, Long id,
    String name, Instant lastExecutionDate, String lastModifiedBy, Instant lastModifiedDate,
    Project project, long projectId, int projectVersion, List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflows,
    List<Tag> tags, int version) {

    public ProjectDeploymentDTO(ProjectDeployment projectDeployment) {
        this(
            projectDeployment.getCreatedBy(), projectDeployment.getCreatedDate(), projectDeployment.getDescription(),
            projectDeployment.isEnabled(), projectDeployment.getEnvironment(), projectDeployment.getId(),
            projectDeployment.getName(), null, projectDeployment.getLastModifiedBy(),
            projectDeployment.getLastModifiedDate(), null, projectDeployment.getProjectId(),
            projectDeployment.getProjectVersion(), List.of(), List.of(), projectDeployment.getVersion());
    }

    public ProjectDeploymentDTO(
        ProjectDeployment projectDeployment, List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflows,
        Project project, Instant lastExecutionDate, List<Tag> tags) {

        this(
            projectDeployment.getCreatedBy(), projectDeployment.getCreatedDate(), projectDeployment.getDescription(),
            projectDeployment.isEnabled(), projectDeployment.getEnvironment(), projectDeployment.getId(),
            projectDeployment.getName(), lastExecutionDate, projectDeployment.getLastModifiedBy(),
            projectDeployment.getLastModifiedDate(), project, projectDeployment.getProjectId(),
            projectDeployment.getProjectVersion(), CollectionUtils.sort(projectDeploymentWorkflows), tags,
            projectDeployment.getVersion());
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProjectDeployment toProjectDeployment() {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setDescription(description);
        projectDeployment.setEnabled(enabled);
        projectDeployment.setEnvironment(environment);
        projectDeployment.setId(id);
        projectDeployment.setName(name);
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);
        projectDeployment.setTags(tags);
        projectDeployment.setVersion(version);

        return projectDeployment;
    }

    public static final class Builder {
        private String createdBy;
        private Instant createdDate;
        private String description;
        private boolean enabled;
        private Environment environment;
        private Long id;
        private String name;
        private Instant lastExecutionDate;
        private String lastModifiedBy;
        private Instant lastModifiedDate;
        private Project project;
        private long projectId;
        private Integer projectVersion;
        private List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflows;
        private List<Tag> tags;
        private int version;

        private Builder() {
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;

            return this;
        }

        public Builder createdDate(Instant createdDate) {
            this.createdDate = createdDate;

            return this;
        }

        public Builder description(String description) {
            this.description = description;

            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;

            return this;
        }

        public Builder environment(Environment environment) {
            this.environment = environment;

            return this;
        }

        public Builder id(Long id) {
            this.id = id;

            return this;
        }

        public Builder name(String name) {
            this.name = name;

            return this;
        }

        public Builder lastExecutionDate(Instant lastExecutionDate) {
            this.lastExecutionDate = lastExecutionDate;

            return this;
        }

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;

            return this;
        }

        public Builder lastModifiedDate(Instant lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;

            return this;
        }

        public Builder project(Project project) {
            this.project = project;

            return this;
        }

        public Builder projectId(long projectId) {
            this.projectId = projectId;

            return this;
        }

        public Builder projectVersion(int projectVersion) {
            this.projectVersion = projectVersion;

            return this;
        }

        public Builder projectDeploymentWorkflows(List<ProjectDeploymentWorkflowDTO> projectDeploymentWorkflows) {
            this.projectDeploymentWorkflows = projectDeploymentWorkflows;

            return this;
        }

        public Builder tags(List<Tag> tags) {
            this.tags = tags;

            return this;
        }

        public Builder version(int version) {
            this.version = version;

            return this;
        }

        public ProjectDeploymentDTO build() {
            return new ProjectDeploymentDTO(
                createdBy, createdDate, description, enabled, environment, id, name, lastExecutionDate, lastModifiedBy,
                lastModifiedDate, project, projectId, projectVersion, projectDeploymentWorkflows, tags, version);
        }
    }
}
