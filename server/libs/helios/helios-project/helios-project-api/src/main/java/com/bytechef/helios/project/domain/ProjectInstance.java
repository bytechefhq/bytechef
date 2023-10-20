
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.helios.project.domain;

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import com.bytechef.tag.domain.Tag;
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
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Ivica Cardic
 */
@Table("project_instance")
public class ProjectInstance implements Persistable<Long> {

    public enum Status {
        DISABLED, ENABLED
    }

    @Column
    private MapWrapper configurationParameters = new MapWrapper();

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private LocalDateTime createdDate;

    @Column
    private String description;

    @Column
    private Status status;

    @Id
    private Long id;

    @Column
    private String name;

    @Column("last_execution_date")
    @LastModifiedDate
    private LocalDateTime lastExecutionDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;

    @Column("project_id")
    private AggregateReference<Project, Long> projectId;

    @MappedCollection(idColumn = "project_instance_id")
    private Set<ProjectInstanceConnection> projectInstanceConnections = new HashSet<>();

    @MappedCollection(idColumn = "project_instance_id")
    private Set<ProjectInstanceTag> projectInstanceTags = new HashSet<>();

    @Version
    private int version;

    public ProjectInstance() {
    }

    @PersistenceCreator
    public ProjectInstance(
        MapWrapper configurationParameters, String description, Long id, String name, LocalDateTime lastExecutionDate,
        AggregateReference<Project, Long> projectId, Set<ProjectInstanceConnection> projectInstanceConnections,
        Set<ProjectInstanceTag> projectInstanceTags, Status status, int version) {

        this.configurationParameters = configurationParameters;
        this.description = description;
        this.id = id;
        this.lastExecutionDate = lastExecutionDate;
        this.name = name;
        this.projectId = projectId;
        this.projectInstanceConnections.addAll(projectInstanceConnections);
        this.projectInstanceTags.addAll(projectInstanceTags);
        this.status = status;
        this.version = version;
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

    public Map<String, Object> getConfigurationParameters() {
        return Collections.unmodifiableMap(configurationParameters.getMap());
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

    public LocalDateTime getLastExecutionDate() {
        return lastExecutionDate;
    }

    public Long getProjectId() {
        return projectId == null ? null : projectId.getId();
    }

    public Status getStatus() {
        return status;
    }

    public List<Long> getTagIds() {
        return projectInstanceTags
            .stream()
            .map(ProjectInstanceTag::getTagId)
            .toList();
    }

    public List<Long> getConnectionIds() {
        return projectInstanceConnections.stream()
            .map(ProjectInstanceConnection::getConnectionId)
            .toList();
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean isNew() {
        return id == null;
    }

    public void setConfigurationParameters(Map<String, Object> configurationParameters) {
        if (configurationParameters != null) {
            this.configurationParameters = new MapWrapper(configurationParameters);
        }
    }

    public void setConnectionIds(List<Long> connectionIds) {
        projectInstanceConnections = new HashSet<>();

        if (connectionIds != null) {
            for (Long connectionId : connectionIds) {
                projectInstanceConnections.add(new ProjectInstanceConnection(connectionId));
            }
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
        this.lastExecutionDate = lastExecutionDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId == null ? null : AggregateReference.to(projectId);
    }

    public void setStatus(Status status) {
        this.status = status;
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
            setTagIds(com.bytechef.commons.util.CollectionUtils.map(tags, Tag::getId));
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
            ", enabled='" + status +
            ", projectId=" + projectId +
            ", description='" + description + '\'' +
            ", parameters=" + configurationParameters +
            ", projectInstanceConnections=" + projectInstanceConnections +
            ", projectInstanceTags=" + projectInstanceTags +
            ", lastExecutionDate=" + lastExecutionDate +
            ", version=" + version +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            '}';
    }

    public ProjectInstance update(ProjectInstance projectInstance) {
        this.id = projectInstance.id;
        this.name = projectInstance.name;
        this.projectId = projectInstance.projectId;
        this.projectInstanceConnections = projectInstance.projectInstanceConnections;
        this.projectInstanceTags = projectInstance.projectInstanceTags;

        return this;
    }
}
