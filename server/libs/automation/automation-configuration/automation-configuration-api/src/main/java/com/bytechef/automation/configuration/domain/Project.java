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
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

    @MappedCollection(idColumn = "project_id")
    private Set<ProjectWorkflow> projectWorkflows = new HashSet<>();

    @Version
    private int version;

    public Project() {
        projectVersions.add(new ProjectVersion(1));
    }

    @PersistenceCreator
    public Project(
        AggregateReference<Category, Long> categoryId, String description, Long id, String name,
        Set<ProjectTag> projectTags, Set<ProjectVersion> projectVersions, Set<ProjectWorkflow> projectWorkflows,
        int version) {

        this.categoryId = categoryId;
        this.description = description;
        this.id = id;
        this.name = name;
        this.projectTags.addAll(projectTags);
        this.projectVersions.addAll(projectVersions);
        this.projectWorkflows.addAll(projectWorkflows);
        this.version = version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void addVersion(List<String> duplicatedVersionWorkflowIds) {
        ProjectVersion projectVersion = getLastProjectVersion();

        int newVersion = projectVersion.getVersion() + 1;

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            if (projectWorkflow.getProjectVersion() == projectVersion.getVersion()) {
                projectWorkflow.setProjectVersion(newVersion);
            }
        }

        projectVersions.add(new ProjectVersion(newVersion));

        for (String workflowId : duplicatedVersionWorkflowIds) {
            projectWorkflows.add(new ProjectWorkflow(workflowId, projectVersion.getVersion()));
        }
    }

    public void addWorkflowId(String workflowId) {
        projectWorkflows.add(new ProjectWorkflow(workflowId, getLastVersion()));
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

    public Map<Integer, List<String>> getAllWorkflowIds() {
        return projectWorkflows.stream()
            .collect(Collectors.groupingBy(
                ProjectWorkflow::getProjectVersion,
                Collectors.collectingAndThen(
                    Collectors.toList(), list -> CollectionUtils.map(list, ProjectWorkflow::getWorkflowId))));
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

    public List<String> getWorkflowIds(int projectVersion) {
        Map<Integer, List<String>> workflowIdMap = getAllWorkflowIds();

        if (workflowIdMap.containsKey(projectVersion)) {
            return workflowIdMap.get(projectVersion);
        } else {
            return List.of();
        }
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

    public void removeWorkflow(String workflowId) {
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

    public void setProjectVersions(List<ProjectVersion> projectVersions) {
        this.projectVersions = new HashSet<>(projectVersions);
    }

    public void setProjectWorkflows(Set<ProjectWorkflow> projectWorkflows) {
        this.projectWorkflows = new HashSet<>(projectWorkflows);
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

    @Override
    public String toString() {
        return "Project{" +
            "id=" + id +
            ", name='" + name + '\'' +
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
        private List<String> workflowIds = List.of();

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
            project.setTagIds(tagIds);
            project.setVersion(version);

            workflowIds.forEach(project::addWorkflowId);

            return project;
        }
    }
}
