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

package com.bytechef.ai.agent.eval.domain;

import com.bytechef.ai.agent.eval.constant.AiAgentEvalRunStatus;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("ai_agent_eval_run")
public final class AiAgentEvalRun {

    @Id
    private Long id;

    private Long agentEvalTestId;

    private String workflowId;

    private String workflowNodeName;

    private Long environmentId;

    private String name;

    @Column("status")
    private int status;

    @Nullable
    private Double averageScore;

    private int totalScenarios;

    private int completedScenarios;

    @Nullable
    private Instant startedDate;

    @Nullable
    private Instant completedDate;

    @Nullable
    private String agentVersion;

    private int totalInputTokens;

    private int totalOutputTokens;

    @CreatedDate
    private Instant createdDate;

    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private Instant lastModifiedDate;

    @LastModifiedBy
    private String lastModifiedBy;

    @Version
    private int version;

    public AiAgentEvalRun() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentEvalTestId() {
        return agentEvalTestId;
    }

    public void setAgentEvalTestId(Long agentEvalTestId) {
        this.agentEvalTestId = agentEvalTestId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowNodeName() {
        return workflowNodeName;
    }

    public void setWorkflowNodeName(String workflowNodeName) {
        this.workflowNodeName = workflowNodeName;
    }

    public Long getEnvironmentId() {
        return environmentId;
    }

    public void setEnvironmentId(Long environmentId) {
        this.environmentId = environmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AiAgentEvalRunStatus getStatus() {
        return AiAgentEvalRunStatus.values()[status];
    }

    public void setStatus(AiAgentEvalRunStatus status) {
        this.status = status.ordinal();
    }

    @Nullable
    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(@Nullable Double averageScore) {
        this.averageScore = averageScore;
    }

    public int getTotalScenarios() {
        return totalScenarios;
    }

    public void setTotalScenarios(int totalScenarios) {
        this.totalScenarios = totalScenarios;
    }

    public int getCompletedScenarios() {
        return completedScenarios;
    }

    public void setCompletedScenarios(int completedScenarios) {
        this.completedScenarios = completedScenarios;
    }

    @Nullable
    public Instant getStartedDate() {
        return startedDate;
    }

    public void setStartedDate(@Nullable Instant startedDate) {
        this.startedDate = startedDate;
    }

    @Nullable
    public Instant getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(@Nullable Instant completedDate) {
        this.completedDate = completedDate;
    }

    @Nullable
    public String getAgentVersion() {
        return agentVersion;
    }

    public void setAgentVersion(@Nullable String agentVersion) {
        this.agentVersion = agentVersion;
    }

    public int getTotalInputTokens() {
        return totalInputTokens;
    }

    public void setTotalInputTokens(int totalInputTokens) {
        this.totalInputTokens = totalInputTokens;
    }

    public int getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public void setTotalOutputTokens(int totalOutputTokens) {
        this.totalOutputTokens = totalOutputTokens;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Instant lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiAgentEvalRun aiAgentEvalRun)) {
            return false;
        }

        return Objects.equals(id, aiAgentEvalRun.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiAgentEvalRun{" +
            "id=" + id +
            ", agentEvalTestId=" + agentEvalTestId +
            ", workflowId='" + workflowId + '\'' +
            ", workflowNodeName='" + workflowNodeName + '\'' +
            ", environmentId=" + environmentId +
            ", name='" + name + '\'' +
            ", status=" + status +
            ", averageScore=" + averageScore +
            ", totalScenarios=" + totalScenarios +
            ", completedScenarios=" + completedScenarios +
            ", startedDate=" + startedDate +
            ", completedDate=" + completedDate +
            ", agentVersion='" + agentVersion + '\'' +
            ", totalInputTokens=" + totalInputTokens +
            ", totalOutputTokens=" + totalOutputTokens +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
