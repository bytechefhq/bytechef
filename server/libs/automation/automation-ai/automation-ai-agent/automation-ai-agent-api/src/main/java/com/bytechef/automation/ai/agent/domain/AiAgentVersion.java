/*
 * Copyright 2025 ByteChef
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

package com.bytechef.automation.ai.agent.domain;

import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("ai_agent_version")
public final class AiAgentVersion {

    @Id
    private Long id;

    @Column("project_id")
    private long projectId;

    @Column("project_version")
    private int projectVersion;

    @Column("project_workflow_uuid")
    private UUID projectWorkflowUuid;

    @Column("definition")
    private String definition;

    public AiAgentVersion() {
    }

    public AiAgentVersion(long projectId, int projectVersion, UUID projectWorkflowUuid, String definition) {
        this.projectId = projectId;
        this.projectVersion = projectVersion;
        this.projectWorkflowUuid = projectWorkflowUuid;
        this.definition = definition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AiAgentVersion agentVersion = (AiAgentVersion) o;

        return Objects.equals(id, agentVersion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public long getProjectId() {
        return projectId;
    }

    public int getProjectVersion() {
        return projectVersion;
    }

    public UUID getProjectWorkflowUuid() {
        return projectWorkflowUuid;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    @Override
    public String toString() {
        return "AiAgentVersion{" +
            "id=" + id +
            ", projectId=" + projectId +
            ", projectVersion=" + projectVersion +
            ", projectWorkflowUuid=" + projectWorkflowUuid +
            '}';
    }
}
