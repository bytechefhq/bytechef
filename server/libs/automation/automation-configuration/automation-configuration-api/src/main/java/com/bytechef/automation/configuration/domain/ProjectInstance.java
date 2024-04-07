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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.tag.domain.Tag;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("project_instance")
public class ProjectInstance {

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String description;

    @Column
    private boolean enabled;

    @Column
    private int environment;

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

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @Column("project_version")
    private int projectVersion;

    @MappedCollection(idColumn = "project_instance_id")
    private Set<ProjectInstanceTag> projectInstanceTags = Collections.emptySet();

    @Version
    private int version;

    public ProjectInstance() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ProjectInstance project = (ProjectInstance) o;

        return Objects.equals(id, project.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
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

    public boolean isEnabled() {
        return enabled;
    }

    public Environment getEnvironment() {
        return Environment.values()[environment];
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

    public Long getProjectId() {
        return projectId == null ? null : projectId.getId();
    }

    public int getProjectVersion() {
        return projectVersion;
    }

    public List<Long> getTagIds() {
        return projectInstanceTags
            .stream()
            .map(ProjectInstanceTag::getTagId)
            .toList();
    }

    public int getVersion() {
        return version;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment.ordinal();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId == null ? null : AggregateReference.to(projectId);
    }

    public void setProjectVersion(int projectVersion) {
        this.projectVersion = projectVersion;
    }

    public void setTagIds(List<Long> tagIds) {
        this.projectInstanceTags = new HashSet<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (Long tagId : tagIds) {
                projectInstanceTags.add(new ProjectInstanceTag(tagId));
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
        return "ProjectInstance{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", enabled='" + enabled +
            ", environment='" + environment + '\'' +
            ", projectId=" + projectId +
            ", description='" + description + '\'' +
            ", projectInstanceTags=" + projectInstanceTags +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }
}
