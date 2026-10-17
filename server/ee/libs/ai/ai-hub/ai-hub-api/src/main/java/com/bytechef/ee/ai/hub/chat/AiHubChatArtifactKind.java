/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

/**
 * Classifies the type of side-effect artifact that was produced during a AI Hub chat turn.
 *
 * <p>
 * <b>Append-only.</b> The values are persisted as INT ordinals via Spring Data JDBC and
 * {@link com.bytechef.ee.ai.hub.util.EnumOrdinals} — reordering or deleting a value would silently re-map every
 * historical row to the wrong kind. New values MUST be appended at the end. The
 * {@code EnumOrdinalStabilityTest#testChatArtifactKindOrdinals} pinning test enforces this at build time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubChatArtifactKind {

    FILE_CREATED,
    BINARY_FILE_CREATED,
    WORKFLOW_CREATED,
    WORKFLOW_UPDATED,
    DATA_TABLE_ROW_ADDED,
    DATA_TABLE_ROW_UPDATED,
    DATA_TABLE_ROW_DELETED,
    DATA_TABLE_COLUMN_ADDED,
    KB_DOCUMENT_ADDED,
    KB_DOCUMENT_DELETED,
    WORKFLOW_EXECUTION_STARTED,
    MEMORY_CREATED,
    MEMORY_UPDATED,
    MEMORY_DELETED,
    MEMORY_RENAMED,

    FILE_REFERENCED,
    WORKFLOW_REFERENCED,
    DATA_TABLE_REFERENCED,
    KB_REFERENCED,

    MCP_SERVER_REFERENCED,
    API_COLLECTION_REFERENCED,
    WORKFLOW_EXECUTION_REFERENCED,
    CHAT_REFERENCED,

    SKILL_REFERENCED,

    CUSTOM_COMPONENT_REFERENCED,

    CODE_WORKFLOW_REFERENCED,

    FILE_UPDATED,

    AI_AGENT_REFERENCED
}
