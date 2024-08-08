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
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectDTO(
    Category category, String createdBy, LocalDateTime createdDate, String description, Long id, String name,
    String lastModifiedBy, LocalDateTime lastModifiedDate, LocalDateTime lastPublishedDate, Status lastStatus,
    int lastVersion, List<ProjectVersion> projectVersions, List<Tag> tags, int version, List<Long> projectWorkflowIds,
    Long workspaceId) {

    public ProjectDTO(Category category, Project project, List<Tag> tags, List<Long> projectWorkflowIds) {
        this(
            category, project.getCreatedBy(), project.getCreatedDate(), project.getDescription(), project.getId(),
            project.getName(), project.getLastModifiedBy(), project.getLastModifiedDate(),
            project.getLastPublishedDate(), project.getLastStatus(), project.getLastVersion(),
            project.getProjectVersions(), tags, project.getVersion(), projectWorkflowIds, project.getWorkspaceId());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Project toProject() {
        Project project = new Project();

        project.setCategory(category);
        project.setDescription(description);
        project.setId(id);
        project.setName(name);
        project.setProjectVersions(projectVersions == null ? List.of() : projectVersions);
        project.setVersion(version);
        project.setTags(tags);
        project.setWorkspaceId(workspaceId);

        return project;
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Category category;
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private Long id;
        private String name;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private LocalDateTime lastPublishedDate;
        private Status lastStatus = Status.DRAFT;
        private int lastVersion;
        private List<Tag> tags;
        private int version;
        private List<ProjectVersion> projectVersions;
        private List<Long> projectWorkflowIds;
        private Long workspaceId;

        private Builder() {
        }

        public Builder category(Category category) {
            this.category = category;

            return this;
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

        public Builder id(Long id) {
            this.id = id;

            return this;
        }

        public Builder name(String name) {
            this.name = name;

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

        public Builder lastVersion(int lastVersion) {
            this.lastVersion = lastVersion;

            return this;
        }

        public Builder lastPublishedDate(LocalDateTime lastPublishedDate) {
            this.lastPublishedDate = lastPublishedDate;

            return this;
        }

        public Builder lastStatus(Status lastStatus) {
            this.lastStatus = lastStatus;

            return this;
        }

        public Builder projectVersions(List<ProjectVersion> projectVersions) {
            this.projectVersions = projectVersions;

            return this;
        }

        public Builder projectWorkflowIds(List<Long> projectWorkflowIds) {
            this.projectWorkflowIds = projectWorkflowIds;

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

        public Builder workspaceId(long workspaceId) {
            this.workspaceId = workspaceId;

            return this;
        }

        public ProjectDTO build() {
            return new ProjectDTO(
                category, createdBy, createdDate, description, id, name, lastModifiedBy, lastModifiedDate,
                lastPublishedDate, lastStatus, lastVersion, projectVersions, tags, version, projectWorkflowIds,
                workspaceId);
        }
    }
}
