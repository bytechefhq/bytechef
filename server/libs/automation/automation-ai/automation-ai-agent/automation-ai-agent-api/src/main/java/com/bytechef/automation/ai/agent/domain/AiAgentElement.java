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
 * Domain class for a building block (tool, sub-agent, knowledge base, etc.) attached to an {@link AiAgent}. The
 * {@link #kind} discriminates the element type; {@link #referenceId} points at the referenced entity (e.g. another
 * {@code AiAgent} when {@code kind = SUB_AGENT}). Rows cascade-delete with their owning agent.
 *
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

    /**
     * Singleton row carrying an {@code aiAgentUtils/v1/approvalGateTool}'s own settings — {@code parameters} may carry
     * {@code approvalExpiresIn} (integer) and {@code approvalExpiresInUnit} ({@code "HOURS"}/{@code "DAYS"}), mirroring
     * {@code AiAgentUtilsApprovalGateTool}'s own properties (see {@code ToolConstants}). Emitted only when at least one
     * {@code TOOL} row has {@code parameters["requiresApproval"] = true} — see {@code AiAgentWorkflowGenerator}.
     */
    public static final String KIND_APPROVAL_GATE = "APPROVAL_GATE";

    // Retired kind: "APPROVAL_CHANNEL". Approval delivery is derived from the agent's own channels
    // (AiAgentWorkflowGenerator.buildApprovalDeliveryChannels), so a separate row is neither written nor read. Rows
    // left over in ai_agent_element are ignored rather than migrated; do not reuse the name for anything else.

    /**
     * Singleton row that pulls the platform's {@code approval/v1/requestApproval} cluster element (
     * {@code ApprovalRequestApprovalTool}, {@code components/approval}) directly into the agent's flat tools array — an
     * LLM-invocable "ask a human" tool, distinct from {@link #KIND_APPROVAL_GATE} (which gates OTHER tools rather than
     * being callable itself). {@code AiAgentUtilsApprovalGateTool.checkGatableChild} rejects nesting
     * {@code requestApproval} inside a gate, so this element is always emitted as a top-level tool, never inside the
     * gate — see {@code AiAgentWorkflowGenerator}. {@code parameters} is currently unused: every property
     * {@code ApprovalRequestApprovalAction} declares ({@code formTitle}, {@code formDescription}, {@code inputs},
     * {@code expiresIn}, {@code expiresInUnit}) is optional, and — being a {@code TOOLS}-type cluster element — they
     * all become the LLM's own function-calling schema at call time, so there is no builder-level value worth
     * pre-filling.
     */
    public static final String KIND_APPROVAL_TOOL = "APPROVAL_TOOL";

    /**
     * Shared {@code parameters} keys, read by both {@code AiAgentWorkflowGenerator} and {@code AiAgentFacadeImpl} so
     * the two can never drift apart.
     */
    // On a TOOL row: optional boolean — when true, pulls the tool into the generated approval gate.
    public static final String PARAM_REQUIRES_APPROVAL = "requiresApproval";
    // On a TOOL or APPROVAL_CHANNEL row: required component name.
    public static final String PARAM_COMPONENT_NAME = "componentName";
    // On an APPROVAL_CHANNEL row: optional cluster element name (only twilio/infobip need it set).
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
