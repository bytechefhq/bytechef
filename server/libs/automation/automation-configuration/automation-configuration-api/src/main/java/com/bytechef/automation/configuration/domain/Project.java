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

import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public final class Project {

    @Column("category_id")
    private AggregateReference<Category, Long> categoryId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String description;

    @Id
    private Long id;

    @Column
    private String name;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @MappedCollection(idColumn = "project_id")
    private Set<ProjectTag> projectTags = new HashSet<>();

    @MappedCollection(idColumn = "project_id")
    private Set<ProjectVersion> projectVersions = new HashSet<>();

    @Version
    private int version;

    @Column("workspace_id")
    private AggregateReference<Category, Long> workspaceId;

    public Project() {
        projectVersions.add(new ProjectVersion(1));
    }

    @PersistenceCreator
    public Project(
        AggregateReference<Category, Long> categoryId, String description, Long id, String name,
        Set<ProjectTag> projectTags, Set<ProjectVersion> projectVersions, int version) {

        this.categoryId = categoryId;
        this.description = description;
        this.id = id;
        this.name = name;
        this.projectTags.addAll(projectTags);
        this.projectVersions.addAll(projectVersions);
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public int addVersion() {
        ProjectVersion projectVersion = getLastProjectVersion();

        int newVersion = projectVersion.getVersion() + 1;

        projectVersions.add(new ProjectVersion(newVersion));

        return newVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Project project = (Project) o;

        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public Long getCategoryId() {
        return categoryId == null ? null : categoryId.getId();
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public ProjectVersion getLastProjectVersion() {
        return projectVersions.stream()
            .max(Comparator.comparingInt(ProjectVersion::getVersion))
            .orElseThrow();
    }

    public LocalDateTime getLastPublishedDate() {
        return getLastProjectVersion().getPublishedDate();
    }

    public Status getLastStatus() {
        return getLastProjectVersion().getStatus();
    }

    public int getLastVersion() {
        return getLastProjectVersion().getVersion();
    }

    public List<ProjectVersion> getProjectVersions() {
        return new ArrayList<>(projectVersions);
    }

    public List<Long> getTagIds() {
        return projectTags
            .stream()
            .map(ProjectTag::getTagId)
            .toList();
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId == null ? null : workspaceId.getId();
    }

    public boolean isPublished() {
        return projectVersions.stream()
            .anyMatch(projectVersion -> projectVersion.getStatus() == Status.PUBLISHED);
    }

    public void publish(String description) {
        ProjectVersion projectVersion = getLastProjectVersion();

        projectVersion.setDescription(description);
        projectVersion.setPublishedDate(LocalDateTime.now());
        projectVersion.setStatus(Status.PUBLISHED);
    }

    public void setCategory(Category category) {
        this.categoryId = category == null
            ? null
            : category.getId() == null ? null : AggregateReference.to(Validate.notNull(category.getId(), "id"));
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId == null ? null : AggregateReference.to(categoryId);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectVersions(List<ProjectVersion> projectVersions) {
        this.projectVersions = new HashSet<>(projectVersions);
    }

    public void setTagIds(List<Long> tagIds) {
        this.projectTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                projectTags.add(new ProjectTag(tagId));
            }
        }
    }

    public void setTags(List<Tag> tags) {
        if (!CollectionUtils.isEmpty(tags)) {
            setTagIds(CollectionUtils.map(tags, Tag::getId));
        }
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId == null ? null : AggregateReference.to(workspaceId);
    }

    @Override
    public String toString() {
        return "Project{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", workspaceId=" + getWorkspaceId() +
            ", categoryId=" + getCategoryId() +
            ", description='" + description + '\'' +
            ", projectTags=" + projectTags +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private Long categoryId;
        private String description;
        private Long id;
        private String name;
        private List<Long> tagIds;
        private int version;
        private long workspaceId;

        private Builder() {
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;

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

        public Builder tagIds(List<Long> tagIds) {
            this.tagIds = tagIds;

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

        public Project build() {
            Project project = new Project();

            if (categoryId != null) {
                project.setCategoryId(categoryId);
            }

            project.setDescription(description);
            project.setId(id);
            project.setName(name);
            project.setTagIds(tagIds);
            project.setVersion(version);
            project.setWorkspaceId(workspaceId);

            return project;
        }
    }
}
