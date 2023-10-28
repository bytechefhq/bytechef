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

package com.bytechef.helios.configuration.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.helios.configuration.domain.Project;
import com.bytechef.helios.configuration.domain.ProjectInstance;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectInstanceDTO(
    String createdBy, LocalDateTime createdDate, String description, boolean enabled, Long id, String name,
    LocalDateTime lastExecutionDate, String lastModifiedBy, LocalDateTime lastModifiedDate, Project project,
    Long projectId, List<ProjectInstanceWorkflow> projectInstanceWorkflows, List<Tag> tags, int version) {

    public ProjectInstanceDTO(
        LocalDateTime lastExecutionDate, ProjectInstance projectInstance,
        List<ProjectInstanceWorkflow> projectInstanceWorkflows, Project project, List<Tag> tags) {

        this(
            projectInstance.getCreatedBy(), projectInstance.getCreatedDate(), projectInstance.getDescription(),
            projectInstance.isEnabled(), projectInstance.getId(), projectInstance.getName(), lastExecutionDate,
            projectInstance.getLastModifiedBy(), projectInstance.getLastModifiedDate(), project,
            projectInstance.getProjectId(), CollectionUtils.sort(projectInstanceWorkflows), tags,
            projectInstance.getVersion());
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProjectInstance toProjectInstance() {
        ProjectInstance projectInstance = new ProjectInstance();

        projectInstance.setDescription(description);
        projectInstance.setEnabled(enabled);
        projectInstance.setId(id);
        projectInstance.setName(name);
        projectInstance.setProjectId(projectId);
        projectInstance.setTags(tags);
        projectInstance.setVersion(version);

        return projectInstance;
    }

    public static final class Builder {
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private boolean enabled;
        private Long id;
        private String name;
        private LocalDateTime lastExecutionDate;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private Project project;
        private Long projectId;
        private List<ProjectInstanceWorkflow> projectInstanceWorkflows;
        private List<Tag> tags;
        private int version;

        private Builder() {
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
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

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lastExecutionDate(LocalDateTime lastExecutionDate) {
            this.lastExecutionDate = lastExecutionDate;
            return this;
        }

        public Builder lastModifiedBy(String lastModifiedBy) {
            this.lastModifiedBy = lastModifiedBy;
            return this;
        }

        public Builder lastModifiedDate(LocalDateTime lastModifiedDate) {
            this.lastModifiedDate = lastModifiedDate;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder projectInstanceWorkflows(List<ProjectInstanceWorkflow> projectInstanceWorkflows) {
            this.projectInstanceWorkflows = projectInstanceWorkflows;
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

        public ProjectInstanceDTO build() {
            return new ProjectInstanceDTO(
                createdBy, createdDate, description, enabled, id, name, lastExecutionDate, lastModifiedBy,
                lastModifiedDate, project, projectId, projectInstanceWorkflows, tags, version);
        }
    }
}
