/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalRule;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactKind;
import com.bytechef.ee.ai.hub.chat.AiHubChatArtifactStatus;
import com.bytechef.ee.ai.hub.chat.AiHubChatStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ordinal of every persisted enum value to a fixed integer. The AI Hub entities store these enums as
 * {@code int} ordinal columns, so a reorder is silent and irreversible: every historical row is re-attributed to
 * whatever value now lives at the old ordinal. When a new value is needed, append it at the end and add it to the
 * corresponding map below.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testChatStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("ACTIVE", 0);
        expected.put("ARCHIVED", 1);
        expected.put("DELETED", 2);

        assertOrdinalsMatch(AiHubChatStatus.values(), expected);
    }

    @Test
    void testChatArtifactStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("APPLIED", 0);
        expected.put("EXPIRED", 1);
        expected.put("IRREVERSIBLE", 2);

        assertOrdinalsMatch(AiHubChatArtifactStatus.values(), expected);
    }

    @Test
    void testChatArtifactKindOrdinalsAreStable() {
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
        expected.put("FILE_REFERENCED", 15);
        expected.put("WORKFLOW_REFERENCED", 16);
        expected.put("DATA_TABLE_REFERENCED", 17);
        expected.put("KB_REFERENCED", 18);
        expected.put("MCP_SERVER_REFERENCED", 19);
        expected.put("API_COLLECTION_REFERENCED", 20);
        expected.put("WORKFLOW_EXECUTION_REFERENCED", 21);
        expected.put("CHAT_REFERENCED", 22);
        expected.put("SKILL_REFERENCED", 23);
        expected.put("CUSTOM_COMPONENT_REFERENCED", 24);
        expected.put("CODE_WORKFLOW_REFERENCED", 25);
        expected.put("FILE_UPDATED", 26);
        expected.put("AI_AGENT_REFERENCED", 27);

        assertOrdinalsMatch(AiHubChatArtifactKind.values(), expected);
    }

    @Test
    void testSourceOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("WORKFLOW_EDITOR", 0);
        expected.put("CODE_EDITOR", 1);
        expected.put("CLUSTER_ELEMENT", 2);
        expected.put("FILES", 3);
        expected.put("AI_HUB", 4);

        assertOrdinalsMatch(Source.values(), expected);
    }

    @Test
    void testModeOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("ASK", 0);
        expected.put("BUILD", 1);

        assertOrdinalsMatch(Mode.values(), expected);
    }

    @Test
    void testToolApprovalStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("PENDING", 0);
        expected.put("APPROVED", 1);
        expected.put("REJECTED", 2);
        expected.put("EXPIRED", 3);
        expected.put("SUPERSEDED", 4);
        expected.put("FAILED", 5);

        assertOrdinalsMatch(AiHubToolApproval.Status.values(), expected);
    }

    @Test
    void testToolApprovalToolKindOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("CATALOG", 0);
        expected.put("COMPONENT", 1);

        assertOrdinalsMatch(AiHubToolApproval.ToolKind.values(), expected);
    }

    @Test
    void testToolApprovalRuleModeOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("REQUIRE", 0);
        expected.put("EXEMPT", 1);

        assertOrdinalsMatch(AiHubToolApprovalRule.Mode.values(), expected);
    }

    private static <E extends Enum<E>> void assertOrdinalsMatch(E[] actualValues, Map<String, Integer> expected) {
        Map<String, Integer> actual = new LinkedHashMap<>();

        for (E value : actualValues) {
            actual.put(value.name(), value.ordinal());
        }

        assertThat(actual).containsExactlyEntriesOf(expected);
    }
}
