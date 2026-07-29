/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

/**
 * Classifies the type of side-effect artifact that was produced during a AI Hub task turn.
 *
 * <p>
 * <b>Append-only.</b> The values are persisted as INT ordinals via Spring Data JDBC and
 * {@link com.bytechef.ee.ai.hub.util.EnumOrdinals} — reordering or deleting a value would silently re-map every
 * historical row to the wrong kind. New values MUST be appended at the end. The
 * {@code EnumOrdinalStabilityTest#testTaskArtifactKindOrdinals} pinning test enforces this at build time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiHubTaskArtifactKind {

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
    // workflow execution, task) copied onto a spawned task from a personal-agent template. Appended at the
    // END per the JDBC enum-storage convention so the ordinals of all earlier values stay pinned.
    MCP_SERVER_REFERENCED,
    API_COLLECTION_REFERENCED,
    WORKFLOW_EXECUTION_REFERENCED,
    TASK_REFERENCED,

    // Agent-opened / composer-referenced skill (SkillsTools archive). Appended at the END per the JDBC
    // enum-storage convention so all earlier ordinals stay pinned.
    SKILL_REFERENCED,

    // Agent-referenced custom component (custom component source). Appended at the END per the JDBC
    // enum-storage convention so all earlier ordinals stay pinned.
    CUSTOM_COMPONENT_REFERENCED
}
