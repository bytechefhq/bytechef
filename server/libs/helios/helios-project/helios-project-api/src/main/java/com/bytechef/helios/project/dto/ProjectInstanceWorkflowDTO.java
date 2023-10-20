
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

import com.bytechef.helios.project.domain.ProjectInstance;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.hermes.connection.domain.Connection;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ProjectInstanceWorkflowDTO(
    List<Long> connectionIds, List<Connection> connections, String createdBy, LocalDateTime createdDate,
    boolean enabled, Long id, Map<String, Object> inputs, LocalDateTime lastExecutionDate,
    String lastModifiedBy, LocalDateTime lastModifiedDate, ProjectInstance projectInstance, Long projectInstanceId,
    int version, String workflowId) {

    public ProjectInstanceWorkflowDTO(
        ProjectInstance projectInstance, ProjectInstanceWorkflow projectInstanceWorkflow,
        List<Connection> connections) {

        this(
            projectInstanceWorkflow.getConnectionIds(), connections, projectInstanceWorkflow.getCreatedBy(),
            projectInstanceWorkflow.getCreatedDate(), projectInstanceWorkflow.isEnabled(),
            projectInstanceWorkflow.getId(), projectInstanceWorkflow.getInputs(),
            projectInstanceWorkflow.getLastExecutionDate(), projectInstanceWorkflow.getLastModifiedBy(),
            projectInstanceWorkflow.getLastModifiedDate(), projectInstance,
            projectInstanceWorkflow.getProjectInstanceId(), projectInstanceWorkflow.getVersion(),
            projectInstanceWorkflow.getWorkflowId());
    }

    public static Builder builder() {
        return new Builder();
    }

    public ProjectInstanceWorkflow toProjectInstanceWorkflow() {
        ProjectInstanceWorkflow projectInstanceWorkflow = new ProjectInstanceWorkflow();

        projectInstanceWorkflow.setConnectionIds(connectionIds);
        projectInstanceWorkflow.setEnabled(enabled);
        projectInstanceWorkflow.setId(id);
        projectInstanceWorkflow.setInputs(inputs);
        projectInstanceWorkflow.setLastExecutionDate(lastExecutionDate);
        projectInstanceWorkflow.setProjectInstanceId(projectInstanceId);
        projectInstanceWorkflow.setVersion(version);
        projectInstanceWorkflow.setWorkflowId(workflowId);

        return projectInstanceWorkflow;
    }

    public static final class Builder {
        private List<Long> connectionIds;
        private List<Connection> connections;
        private String createdBy;
        private LocalDateTime createdDate;
        private boolean enabled;
        private Long id;
        private Map<String, Object> inputs;
        private LocalDateTime lastExecutionDate;
        private String lastModifiedBy;
        private LocalDateTime lastModifiedDate;
        private ProjectInstance projectInstance;
        private Long projectInstanceId;
        private int version;
        private String workflowId;

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

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder createdDate(LocalDateTime createdDate) {
            this.createdDate = createdDate;
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

        public Builder inputs(Map<String, Object> inputs) {
            this.inputs = inputs;
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

        public Builder projectInstance(ProjectInstance projectInstance) {
            this.projectInstance = projectInstance;
            return this;
        }

        public Builder projectInstanceId(Long projectInstanceId) {
            this.projectInstanceId = projectInstanceId;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder workflowId(String workflowId) {
            this.workflowId = workflowId;
            return this;
        }

        public ProjectInstanceWorkflowDTO build() {
            return new ProjectInstanceWorkflowDTO(
                connectionIds, connections, createdBy, createdDate, enabled, id, inputs, lastExecutionDate,
                lastModifiedBy, lastModifiedDate, projectInstance, projectInstanceId, version, workflowId);
        }
    }
}
