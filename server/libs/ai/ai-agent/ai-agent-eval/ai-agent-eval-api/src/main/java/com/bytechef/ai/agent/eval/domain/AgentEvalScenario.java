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

import com.bytechef.ai.agent.eval.constant.AgentScenarioType;
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
@Table("agent_eval_scenario")
public final class AgentEvalScenario {

    @Id
    private Long id;

    private Long agentEvalTestId;

    private String name;

    @Column("type")
    private int type;

    @Nullable
    private String userMessage;

    @Nullable
    private String expectedOutput;

    @Nullable
    private String personaPrompt;

    private int maxTurns;

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

    public AgentEvalScenario() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AgentScenarioType getType() {
        return AgentScenarioType.values()[type];
    }

    public void setType(AgentScenarioType type) {
        this.type = type.ordinal();
    }

    @Nullable
    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(@Nullable String userMessage) {
        this.userMessage = userMessage;
    }

    @Nullable
    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(@Nullable String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    @Nullable
    public String getPersonaPrompt() {
        return personaPrompt;
    }

    public void setPersonaPrompt(@Nullable String personaPrompt) {
        this.personaPrompt = personaPrompt;
    }

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
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

        if (!(object instanceof AgentEvalScenario agentEvalScenario)) {
            return false;
        }

        return Objects.equals(id, agentEvalScenario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AgentEvalScenario{" +
            "id=" + id +
            ", agentEvalTestId=" + agentEvalTestId +
            ", name='" + name + '\'' +
            ", type=" + type +
            ", userMessage='" + userMessage + '\'' +
            ", expectedOutput='" + expectedOutput + '\'' +
            ", personaPrompt='" + personaPrompt + '\'' +
            ", maxTurns=" + maxTurns +
            ", createdDate=" + createdDate +
            ", createdBy='" + createdBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", version=" + version +
            '}';
    }
}
