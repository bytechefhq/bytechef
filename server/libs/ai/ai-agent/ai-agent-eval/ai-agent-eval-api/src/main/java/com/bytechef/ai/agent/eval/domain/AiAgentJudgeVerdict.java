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

import com.bytechef.ai.agent.eval.constant.AiAgentJudgeScope;
import com.bytechef.ai.agent.eval.constant.AiAgentJudgeType;
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
@Table("ai_agent_judge_verdict")
public final class AiAgentJudgeVerdict {

    @Id
    private Long id;

    private Long agentEvalResultId;

    private String judgeName;

    @Column("judge_type")
    private int judgeType;

    @Column("judge_scope")
    private int judgeScope;

    private boolean passed;

    private double score;

    @Nullable
    private String explanation;

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

    public AiAgentJudgeVerdict() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAgentEvalResultId() {
        return agentEvalResultId;
    }

    public void setAgentEvalResultId(Long agentEvalResultId) {
        this.agentEvalResultId = agentEvalResultId;
    }

    public String getJudgeName() {
        return judgeName;
    }

    public void setJudgeName(String judgeName) {
        this.judgeName = judgeName;
    }

    public AiAgentJudgeType getJudgeType() {
        return AiAgentJudgeType.values()[judgeType];
    }

    public void setJudgeType(AiAgentJudgeType judgeType) {
        this.judgeType = judgeType.ordinal();
    }

    public AiAgentJudgeScope getJudgeScope() {
        return AiAgentJudgeScope.values()[judgeScope];
    }

    public void setJudgeScope(AiAgentJudgeScope judgeScope) {
        this.judgeScope = judgeScope.ordinal();
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Nullable
    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(@Nullable String explanation) {
        this.explanation = explanation;
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

        if (!(object instanceof AiAgentJudgeVerdict agentJudgeVerdict)) {
            return false;
        }

        return Objects.equals(id, agentJudgeVerdict.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiAgentJudgeVerdict{" +
            "id=" + id +
            ", agentEvalResultId=" + agentEvalResultId +
            ", judgeName='" + judgeName + '\'' +
            ", judgeType=" + judgeType +
            ", judgeScope=" + judgeScope +
            ", passed=" + passed +
            ", score=" + score +
            ", explanation='" + explanation + '\'' +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
