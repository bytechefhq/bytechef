/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactKind;

/**
 * Classifies a resource a Personal Agent has pre-declared in its template — one of the AI Hub composer's
 * reference-style resource kinds. Tools are NOT in this enum: a tool is a (component, version, operation) triple
 * persisted in {@code ai_hub_personal_agent_tool}, structurally different from a flat (kind, id, name) reference.
 *
 * <p>
 * <b>Append-only.</b> Persisted as an INT ordinal on {@code ai_hub_personal_agent_resource.kind}. Reordering or
 * deleting a value silently re-maps every historical row. New values MUST be appended at the end;
 * {@code EnumOrdinalStabilityTest#testPersonalAgentResourceKindOrdinals} enforces this at build time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubPersonalAgentResourceKind {

    // append-only
    WORKFLOW,
    FILE,
    DATA_TABLE,
    KNOWLEDGE_BASE,
    MCP_SERVER,
    API_COLLECTION,
    WORKFLOW_EXECUTION,
    TASK;

    /**
     * Maps this agent resource kind to the {@link AiHubTaskArtifactKind} reference kind the task-spawn copy path
     * records it under.
     */
    public AiHubTaskArtifactKind toArtifactKind() {
        return switch (this) {
            case WORKFLOW -> AiHubTaskArtifactKind.WORKFLOW_REFERENCED;
            case FILE -> AiHubTaskArtifactKind.FILE_REFERENCED;
            case DATA_TABLE -> AiHubTaskArtifactKind.DATA_TABLE_REFERENCED;
            case KNOWLEDGE_BASE -> AiHubTaskArtifactKind.KB_REFERENCED;
            case MCP_SERVER -> AiHubTaskArtifactKind.MCP_SERVER_REFERENCED;
            case API_COLLECTION -> AiHubTaskArtifactKind.API_COLLECTION_REFERENCED;
            case WORKFLOW_EXECUTION -> AiHubTaskArtifactKind.WORKFLOW_EXECUTION_REFERENCED;
            case TASK -> AiHubTaskArtifactKind.TASK_REFERENCED;
        };
    }
}
