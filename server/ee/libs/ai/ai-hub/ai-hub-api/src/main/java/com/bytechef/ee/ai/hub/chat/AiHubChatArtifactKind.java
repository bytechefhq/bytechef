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

    // append-only
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

    // User-attached references via the composer plus-button menu — no side effect to undo on the underlying
    // entity. Appended at the END of the enum to preserve ordinal stability per the JDBC enum-storage
    // convention (new values get the next ordinals; existing rows keep their ordinals stable).
    FILE_REFERENCED,
    WORKFLOW_REFERENCED,
    DATA_TABLE_REFERENCED,
    KB_REFERENCED,

    // Agent-template referenced resources — the four composer resource kinds (MCP server, API collection,
    // workflow execution, chat) copied onto a spawned chat from a task. Appended at the
    // END per the JDBC enum-storage convention so the ordinals of all earlier values stay pinned.
    MCP_SERVER_REFERENCED,
    API_COLLECTION_REFERENCED,
    WORKFLOW_EXECUTION_REFERENCED,
    CHAT_REFERENCED,

    // Agent-opened / composer-referenced skill (SkillsTools archive). Appended at the END per the JDBC
    // enum-storage convention so all earlier ordinals stay pinned.
    SKILL_REFERENCED,

    // Agent-referenced custom component (custom component source). Appended at the END per the JDBC
    // enum-storage convention so all earlier ordinals stay pinned.
    CUSTOM_COMPONENT_REFERENCED,

    // Agent-referenced code workflow (code workflow source). Appended at the END per the JDBC
    // enum-storage convention so all earlier ordinals stay pinned.
    CODE_WORKFLOW_REFERENCED,

    // Edit-in-place of an existing asset file via the updateAssetFileContent tool. Appended at the END per the
    // JDBC enum-storage convention so all earlier ordinals stay pinned.
    FILE_UPDATED,

    // Agent-referenced AI Agent (automation-ai-agent) opened via openResourceTab. Appended at the END per the
    // JDBC enum-storage convention so all earlier ordinals stay pinned.
    AI_AGENT_REFERENCED
}
