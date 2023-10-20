
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

package com.bytechef.helios.project.dto;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.helios.project.domain.Project;
import com.bytechef.helios.project.domain.ProjectInstance;
import com.bytechef.tag.domain.Tag;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectInstanceDTO(
    List<Long> connectionIds, List<Connection> connections, Map<String, Object> configurationParameters,
    String createdBy, LocalDateTime createdDate, String description, Long id, String name,
    LocalDateTime lastExecutionDate, String lastModifiedBy, LocalDateTime lastModifiedDate, Long projectId,
    Project project, ProjectInstance.Status status, List<Tag> tags, int version) {

    public ProjectInstanceDTO(ProjectInstance projectInstance, List<Tag> tags) {
        this(projectInstance, null, null, tags);
    }

    public ProjectInstanceDTO(ProjectInstance projectInstance, Project project, List<Tag> tags) {
        this(projectInstance, null, project, tags);
    }

    public ProjectInstanceDTO(
        ProjectInstance projectInstance, List<Connection> connections, Project project, List<Tag> tags) {

        this(
            projectInstance.getConnectionIds(), connections, projectInstance.getConfigurationParameters(),
            projectInstance.getCreatedBy(), projectInstance.getCreatedDate(), projectInstance.getDescription(),
            projectInstance.getId(), projectInstance.getName(), projectInstance.getLastExecutionDate(),
            projectInstance.getLastModifiedBy(), projectInstance.getLastModifiedDate(),
            projectInstance.getProjectId(), project, projectInstance.getStatus(), tags, projectInstance.getVersion());
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProjectInstance toProjectInstance() {
        ProjectInstance projectInstance = new ProjectInstance();

        projectInstance.setConnectionIds(connectionIds);
        projectInstance.setConfigurationParameters(configurationParameters);
        projectInstance.setDescription(description);
        projectInstance.setId(id);
        projectInstance.setName(name);
        projectInstance.setLastExecutionDate(lastExecutionDate);
        projectInstance.setProjectId(projectId);
        projectInstance.setStatus(status);
        projectInstance.setTags(tags);
        projectInstance.setVersion(version);

        return projectInstance;
    }

    @SuppressFBWarnings("EI")
    public static final class Builder {
        private List<Long> connectionIds;
        private List<Connection> connections;
        private Map<String, Object> configurationParameters;
        private String createdBy;
        private LocalDateTime createdDate;
        private String description;
        private Long id;
        private String name;
        private LocalDateTime lastExecutionDate;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private Long projectId;
        private Project project;
        private ProjectInstance.Status status;
        private List<Tag> tags;
        private int version;

        private Builder() {
        }

        public Builder connectionIds(List<Long> connectionIds) {
            this.connectionIds = connectionIds;
            return this;
        }

        public Builder connections(List<Connection> connections) {
            this.connections = connections;
            return this;
        }

        public Builder configurationParameters(Map<String, Object> configurationParameters) {
            this.configurationParameters = configurationParameters;
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

        public Builder projectId(Long projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder project(Project project) {
            this.project = project;
            return this;
        }

        public Builder status(ProjectInstance.Status status) {
            this.status = status;
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
                connectionIds, connections, configurationParameters, createdBy, createdDate, description, id, name,
                lastExecutionDate, lastModifiedBy, lastModifiedDate, projectId, project, status, tags, version);
        }
    }
}
