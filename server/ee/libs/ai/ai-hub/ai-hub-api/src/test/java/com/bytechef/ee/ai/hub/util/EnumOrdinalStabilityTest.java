/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResourceKind;
import com.bytechef.ee.ai.hub.personalagent.ScheduleFrequencyKind;
import com.bytechef.ee.ai.hub.personalagent.ScheduleLifecycleKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactStatus;
import com.bytechef.ee.ai.hub.task.AiHubTaskKind;
import com.bytechef.ee.ai.hub.task.AiHubTaskStatus;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ordinal of every persisted enum value to a fixed integer. The AI Hub entities (
 * {@link com.bytechef.ee.ai.hub.task.AiHubTask}, {@code AiHubTaskArtifact}, {@code AiAutoMemory}, {@code AiHubUsage})
 * store these enums as {@code int} ordinal columns. A reorder is silent and irreversible — every historical row is
 * re-attributed to whatever value now lives at the old ordinal.
 *
 * <p>
 * <strong>This test fails on any reorder, rename, or removal.</strong> When you legitimately need to add a new value,
 * <em>append it at the end</em> and add a new entry to the corresponding map below — that's the only sanctioned change.
 * If you need to remove or reorder a value, you must first write a Liquibase data migration that converts the column to
 * a stable string code (the architectural fix tracked in I7).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testTaskStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("ACTIVE", 0);
        expected.put("ARCHIVED", 1);
        expected.put("DELETED", 2);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubTaskStatus.values(), expected, AiHubTaskStatus.class.getSimpleName());
    }

    @Test
    void testTaskArtifactStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("APPLIED", 0);
        expected.put("EXPIRED", 1);
        expected.put("IRREVERSIBLE", 2);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubTaskArtifactStatus.values(), expected,
            AiHubTaskArtifactStatus.class.getSimpleName());
    }

    @Test
    void testTaskArtifactKindOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("FILE_CREATED", 0);
        expected.put("BINARY_FILE_CREATED", 1);
        expected.put("WORKFLOW_CREATED", 2);
        expected.put("WORKFLOW_UPDATED", 3);
        expected.put("DATA_TABLE_ROW_ADDED", 4);
        expected.put("DATA_TABLE_ROW_UPDATED", 5);
        expected.put("DATA_TABLE_ROW_DELETED", 6);
        expected.put("DATA_TABLE_COLUMN_ADDED", 7);
        expected.put("KB_DOCUMENT_ADDED", 8);
        expected.put("KB_DOCUMENT_DELETED", 9);
        expected.put("WORKFLOW_EXECUTION_STARTED", 10);
        expected.put("MEMORY_CREATED", 11);
        expected.put("MEMORY_UPDATED", 12);
        expected.put("MEMORY_DELETED", 13);
        expected.put("MEMORY_RENAMED", 14);
        // User-attached references (composer plus-button menu / chat-opened tabs) — appended at the end
        // per the JDBC enum-storage convention to preserve ordinal stability of all earlier values.
        expected.put("FILE_REFERENCED", 15);
        expected.put("WORKFLOW_REFERENCED", 16);
        expected.put("DATA_TABLE_REFERENCED", 17);
        expected.put("KB_REFERENCED", 18);
        // Agent-template referenced resources — appended at the end.
        expected.put("MCP_SERVER_REFERENCED", 19);
        expected.put("API_COLLECTION_REFERENCED", 20);
        expected.put("WORKFLOW_EXECUTION_REFERENCED", 21);
        expected.put("TASK_REFERENCED", 22);
        // Agent-opened / composer-referenced skill (SkillsTools archive). Appended at the END per the JDBC
        // enum-storage convention so all earlier ordinals stay pinned.
        expected.put("SKILL_REFERENCED", 23);
        // Agent-referenced custom component (custom component source). Appended at the END per the JDBC
        // enum-storage convention so all earlier ordinals stay pinned.
        expected.put("CUSTOM_COMPONENT_REFERENCED", 24);
        // Agent-referenced code workflow (code workflow source). Appended at the END per the JDBC
        // enum-storage convention so all earlier ordinals stay pinned.
        expected.put("CODE_WORKFLOW_REFERENCED", 25);
        // Edit-in-place of an existing asset file via the updateAssetFileContent tool. Appended at the END per
        // the JDBC enum-storage convention so all earlier ordinals stay pinned.
        expected.put("FILE_UPDATED", 26);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubTaskArtifactKind.values(), expected,
            AiHubTaskArtifactKind.class.getSimpleName());
    }

    @Test
    void testTaskKindOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        // STANDARD MUST stay at 0 — the column has DEFAULT 0 in Liquibase so every pre-existing row inherits this
        // value. Reordering would silently re-attribute every task row's kind on the next read.
        expected.put("STANDARD", 0);
        expected.put("WORKFLOW_CHAT", 1);
        expected.put("PERSONAL_AGENT", 2);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubTaskKind.values(), expected, AiHubTaskKind.class.getSimpleName());
    }

    @Test
    void testAiAutoMemoryTypeOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("USER", 0);
        expected.put("FEEDBACK", 1);
        expected.put("PROJECT", 2);
        expected.put("REFERENCE", 3);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiAutoMemoryType.values(), expected, AiAutoMemoryType.class.getSimpleName());
    }

    @Test
    void testAiAutoMemoryPrincipalTypeOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("USER", 0);
        expected.put("DEPLOYMENT", 1);
        expected.put("INTEGRATION_INSTANCE", 2);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiAutoMemoryPrincipalType.values(), expected, AiAutoMemoryPrincipalType.class.getSimpleName());
    }

    @Test
    void testSourceOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("WORKFLOW_EDITOR", 0);
        expected.put("CODE_EDITOR", 1);
        expected.put("CLUSTER_ELEMENT", 2);
        expected.put("FILES", 3);
        expected.put("AI_HUB", 4);

        OrdinalStabilityAssertions.assertOrdinalsMatch(Source.values(), expected, Source.class.getSimpleName());
    }

    @Test
    void testModeOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("ASK", 0);
        expected.put("BUILD", 1);

        OrdinalStabilityAssertions.assertOrdinalsMatch(Mode.values(), expected, Mode.class.getSimpleName());
    }

    @Test
    void testScheduleFrequencyKindOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("EVERY_X_MINUTES", 0);
        expected.put("HOURLY", 1);
        expected.put("DAILY", 2);
        expected.put("WEEKLY", 3);
        expected.put("MONTHLY", 4);
        expected.put("CUSTOM_CRON", 5);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            ScheduleFrequencyKind.values(), expected, ScheduleFrequencyKind.class.getSimpleName());
    }

    @Test
    void testScheduleLifecycleKindOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("RECURRING", 0);
        expected.put("NUMBER_OF_RUNS", 1);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            ScheduleLifecycleKind.values(), expected, ScheduleLifecycleKind.class.getSimpleName());
    }

    @Test
    void testPersonalAgentResourceKindOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("WORKFLOW", 0);
        expected.put("FILE", 1);
        expected.put("DATA_TABLE", 2);
        expected.put("KNOWLEDGE_BASE", 3);
        expected.put("MCP_SERVER", 4);
        expected.put("API_COLLECTION", 5);
        expected.put("WORKFLOW_EXECUTION", 6);
        expected.put("TASK", 7);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            AiHubPersonalAgentResourceKind.values(), expected,
            AiHubPersonalAgentResourceKind.class.getSimpleName());
    }
}
