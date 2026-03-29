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

import com.bytechef.ai.agent.eval.constant.AgentEvalResultStatus;
import com.bytechef.file.storage.domain.FileEntry;
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
@Table("agent_eval_result")
public final class AgentEvalResult {

    @Id
    private Long id;

    private Long agentEvalRunId;

    private Long agentEvalScenarioId;

    @Column("status")
    private int status;

    @Nullable
    private Double score;

    @Nullable
    private String errorMessage;

    // Stores a serialized FileEntry identifier (the result of FileEntry.toId());
    // use getTranscriptFileEntry()/setTranscriptFileEntry() for domain-level access
    @Column("transcript_file")
    @Nullable
    private String transcriptFile;

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

    public AgentEvalResult() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentEvalRunId() {
        return agentEvalRunId;
    }

    public void setAgentEvalRunId(Long agentEvalRunId) {
        this.agentEvalRunId = agentEvalRunId;
    }

    public Long getAgentEvalScenarioId() {
        return agentEvalScenarioId;
    }

    public void setAgentEvalScenarioId(Long agentEvalScenarioId) {
        this.agentEvalScenarioId = agentEvalScenarioId;
    }

    public AgentEvalResultStatus getStatus() {
        return AgentEvalResultStatus.values()[status];
    }

    public void setStatus(AgentEvalResultStatus status) {
        this.status = status.ordinal();
    }

    @Nullable
    public Double getScore() {
        return score;
    }

    public void setScore(@Nullable Double score) {
        this.score = score;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Nullable
    public String getTranscriptFile() {
        return transcriptFile;
    }

    @Nullable
    public FileEntry getTranscriptFileEntry() {
        if (transcriptFile == null) {
            return null;
        }

        return FileEntry.parse(transcriptFile);
    }

    public void setTranscriptFileEntry(@Nullable FileEntry fileEntry) {
        if (fileEntry == null) {
            this.transcriptFile = null;
        } else {
            this.transcriptFile = fileEntry.toId();
        }
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

        if (!(object instanceof AgentEvalResult agentEvalResult)) {
            return false;
        }

        return Objects.equals(id, agentEvalResult.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AgentEvalResult{" +
            "id=" + id +
            ", agentEvalRunId=" + agentEvalRunId +
            ", agentEvalScenarioId=" + agentEvalScenarioId +
            ", status=" + status +
            ", score=" + score +
            ", errorMessage='" + errorMessage + '\'' +
            ", transcriptFile='" + transcriptFile + '\'' +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
