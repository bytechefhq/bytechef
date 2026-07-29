/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Pins the wire-format names of {@link AiHubTaskArtifactKind} against a hard-coded snapshot. The companion TS file at
 * {@code client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts} declares a {@code AiHubArtifactKindType}
 * string-union literal that mirrors this enum. Drift between the two sides is silent and load-bearing — adding a kind
 * on the server without updating the TS literal causes the UI to dispatch {@code default} on the unrecognized string,
 * which silently disables the artifact's row in the audit and sidebar viewers (the comment at the top of the TS file
 * explicitly flags this as "a silent type-drift bug").
 *
 * <p>
 * This test fails on any add, rename, or removal. When you legitimately change the enum, update BOTH this snapshot AND
 * the TS file. Until either an OpenAPI codegen pipeline or a sibling client-side test consuming this list lands, this
 * snapshot is the only mechanical check that the two sides stay synchronized. The complementary
 * {@link com.bytechef.ee.ai.hub.util.EnumOrdinalStabilityTest#testTaskArtifactKindOrdinalsAreStable} catches
 * reorderings; this catches name drift.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubTaskArtifactKindWireFormatTest {

    /**
     * The complete set of wire-format names that the client TS literal MUST also declare. Keep this sorted
     * alphabetically (matches the TS file's union ordering) so a diff highlights additions and removals.
     */
    private static final Set<String> EXPECTED_WIRE_NAMES = new TreeSet<>(Set.of(
        "API_COLLECTION_REFERENCED",
        "BINARY_FILE_CREATED",
        "CUSTOM_COMPONENT_REFERENCED",
        "DATA_TABLE_COLUMN_ADDED",
        "DATA_TABLE_REFERENCED",
        "DATA_TABLE_ROW_ADDED",
        "DATA_TABLE_ROW_DELETED",
        "DATA_TABLE_ROW_UPDATED",
        "FILE_CREATED",
        "FILE_REFERENCED",
        "KB_DOCUMENT_ADDED",
        "KB_DOCUMENT_DELETED",
        "KB_REFERENCED",
        "MCP_SERVER_REFERENCED",
        "MEMORY_CREATED",
        "MEMORY_DELETED",
        "MEMORY_RENAMED",
        "MEMORY_UPDATED",
        "SKILL_REFERENCED",
        "TASK_REFERENCED",
        "WORKFLOW_CREATED",
        "WORKFLOW_EXECUTION_REFERENCED",
        "WORKFLOW_EXECUTION_STARTED",
        "WORKFLOW_REFERENCED",
        "WORKFLOW_UPDATED"));

    @Test
    void testWireFormatNamesMatchClientSnapshot() {
        Set<String> actual = new TreeSet<>();

        Stream.of(AiHubTaskArtifactKind.values())
            .map(Enum::name)
            .forEach(actual::add);

        assertThat(actual)
            .as(
                "AiHubTaskArtifactKind wire-format drift: update both this snapshot AND " +
                    "client/src/pages/automation/ai-hub/tasks/api/tasks.api.ts " +
                    "(AiHubArtifactKindType) when adding/renaming/removing values")
            .isEqualTo(EXPECTED_WIRE_NAMES);
    }
}
