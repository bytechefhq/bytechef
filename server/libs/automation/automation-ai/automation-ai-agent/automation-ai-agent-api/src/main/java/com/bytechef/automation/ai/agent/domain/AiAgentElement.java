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

import com.bytechef.commons.data.jdbc.wrapper.MapWrapper;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 */
@Table("ai_agent_element")
public final class AiAgentElement {

    public static final String KIND_MODEL = "MODEL";
    public static final String KIND_TOOL = "TOOL";
    public static final String KIND_SKILL = "SKILL";
    public static final String KIND_SUB_AGENT = "SUB_AGENT";
    public static final String KIND_KNOWLEDGE_BASE = "KNOWLEDGE_BASE";
    public static final String KIND_CHAT_MEMORY = "CHAT_MEMORY";

    public static final String KIND_APPROVAL_GATE = "APPROVAL_GATE";

    public static final String KIND_APPROVAL_TOOL = "APPROVAL_TOOL";

    public static final String PARAM_REQUIRES_APPROVAL = "requiresApproval";
    public static final String PARAM_COMPONENT_NAME = "componentName";
    public static final String PARAM_ELEMENT_NAME = "elementName";

    @Id
    private Long id;

    @Column("agent_id")
    private long agentId;

    @Column("kind")
    private String kind;

    @Column("reference_id")
    private @Nullable Long referenceId;

    @Column
    private MapWrapper parameters = new MapWrapper();

    @Column("connection_id")
    private @Nullable Long connectionId;

    @Column("position")
    private int position;

    public AiAgentElement() {
    }

    public AiAgentElement(long agentId, String kind) {
        this.agentId = agentId;
        this.kind = kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AiAgentElement that = (AiAgentElement) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAgentId() {
        return agentId;
    }

    public void setAgentId(long agentId) {
        this.agentId = agentId;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public @Nullable Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(@Nullable Long referenceId) {
        this.referenceId = referenceId;
    }

    public Map<String, ?> getParameters() {
        return parameters.getMap();
    }

    public void setParameters(Map<String, ?> parameters) {
        this.parameters = new MapWrapper(parameters);
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "AiAgentElement{" +
            "id=" + id +
            ", agentId=" + agentId +
            ", kind='" + kind + '\'' +
            ", referenceId=" + referenceId +
            ", parameters=" + parameters +
            ", connectionId=" + connectionId +
            ", position=" + position +
            '}';
    }
}
