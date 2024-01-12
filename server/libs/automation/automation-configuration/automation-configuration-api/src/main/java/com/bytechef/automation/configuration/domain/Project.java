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

import com.bytechef.category.domain.Category;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Persistable;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table
public final class Project implements Persistable<Long> {

    public enum Status {
        PUBLISHED(1), UNPUBLISHED(0);

        private final int id;

        Status(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public static Status valueOf(int id) {
            return switch (id) {
                case 0 -> Status.UNPUBLISHED;
                case 1 -> Status.PUBLISHED;
                default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
            };
        }
    }

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

    @Column
    private int projectVersion;

    @MappedCollection(idColumn = "project_id")
    private Set<ProjectWorkflow> projectWorkflows = new HashSet<>();

    @Column("published_date")
    private LocalDateTime publishedDate;

    @Column
    private int status;

    @Version
    private int version;

    public Project() {
    }

    @PersistenceCreator
    public Project(
        AggregateReference<Category, Long> categoryId, String description, Long id, String name,
        Set<ProjectTag> projectTags, int projectVersion, Set<ProjectWorkflow> projectWorkflows,
        LocalDateTime publishedDate, int status, int version) {

        this.categoryId = categoryId;
        this.description = description;
        this.id = id;
        this.name = name;
        this.projectTags.addAll(projectTags);
        this.projectVersion = projectVersion;
        this.projectWorkflows.addAll(projectWorkflows);
        this.publishedDate = publishedDate;
        this.status = status;
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addWorkflowId(String workflowId) {
        projectWorkflows.add(new ProjectWorkflow(workflowId));
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

    @Override
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

    public int getProjectVersion() {
        return projectVersion;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public Status getStatus() {
        return Status.valueOf(status);
    }

    public List<Long> getTagIds() {
        return projectTags
            .stream()
            .map(ProjectTag::getTagId)
            .toList();
    }

    public List<String> getWorkflowIds() {
        return CollectionUtils.map(projectWorkflows, ProjectWorkflow::getWorkflowId);
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void removeWorkflow(String workflowId) {
        projectWorkflows.stream()
            .filter(integrationWorkflow -> Objects.equals(integrationWorkflow.getWorkflowId(), workflowId))
            .findFirst()
            .ifPresent(projectWorkflows::remove);
    }

    public void removeWorkflowId(String workflowId) {
        projectWorkflows.stream()
            .filter(projectWorkflow -> Objects.equals(projectWorkflow.getWorkflowId(), workflowId))
            .findFirst()
            .ifPresent(projectWorkflows::remove);
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

    public void setProjectVersion(int projectVersion) {
        this.projectVersion = projectVersion;
    }

    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setStatus(Status status) {
        Validate.notNull(status, "'status' must not be null");

        this.status = status.getId();
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

    public void setWorkflowIds(List<String> workflowIds) {
        projectWorkflows = new HashSet<>();

        if (!CollectionUtils.isEmpty(workflowIds)) {
            for (String workflowId : workflowIds) {
                addWorkflowId(workflowId);
            }
        }
    }

    @Override
    public String toString() {
        return "Project{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", projectVersion='" + projectVersion + '\'' +
            ", status='" + status + '\'' +
            ", lastPublishedDate='" + publishedDate + '\'' +
            ", categoryId=" + getCategoryId() +
            ", description='" + description + '\'' +
            ", projectTags=" + projectTags +
            ", projectWorkflows=" + projectWorkflows +
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
        private LocalDateTime publishedDate;
        private int projectVersion;
        private Status status;
        private List<Long> tagIds;
        private int version;
        private List<String> workflowIds;

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

        public Builder publishedDate(LocalDateTime publishedDate) {
            this.publishedDate = publishedDate;
            return this;
        }

        public Builder projectVersion(int projectVersion) {
            this.projectVersion = projectVersion;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
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

        public Builder workflowIds(List<String> workflowIds) {
            this.workflowIds = workflowIds;

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
            project.setPublishedDate(publishedDate);
            project.setProjectVersion(projectVersion);
            project.setStatus(status);
            project.setTagIds(tagIds);
            project.setVersion(version);
            project.setWorkflowIds(workflowIds);

            return project;
        }
    }
}
