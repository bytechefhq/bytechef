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

package com.bytechef.test.assertion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared AssertJ helper for pinning enum ordinals to fixed integer values.
 *
 * <p>
 * Many ByteChef tables persist enum columns as {@code int} ordinals (see {@code automation-asset-file},
 * {@code ai-copilot}'s {@code AiHubTask}/{@code AiHubTaskArtifact}/{@code AiAutoMemory}/ {@code AiHubUsage} entities).
 * A reorder, rename, or removal of an enum constant silently re-attributes every historical row to a different value at
 * the same ordinal — there is no DB-side guard. The only protection is a unit test that pins the ordinal of every value
 * and fails on any change.
 * </p>
 *
 * <p>
 * This helper lifts the previously-duplicated assertion ceremony out of {@code AssetFileSourceOrdinalStabilityTest} and
 * {@code EnumOrdinalStabilityTest} so a third (and Nth) consumer can pin its enum without copy-pasting the AssertJ
 * chain — and so a refactor that changes the failure message stays consistent across modules.
 * </p>
 */
public final class OrdinalStabilityAssertions {

    private OrdinalStabilityAssertions() {
    }

    /**
     * Pins the ordinals of {@code actualValues} to {@code expectedNameToOrdinal}. Both the names and the ordinals are
     * compared exactly — a reorder, rename, or removal fails the assertion. The expected map's iteration order is
     * preserved in the failure message so reviewers can see the intended ordinal-to-name layout.
     *
     * @param actualValues          the result of {@code SomeEnum.values()} for the enum under test
     * @param expectedNameToOrdinal a {@link LinkedHashMap}-shaped map of {@code "VALUE_NAME" -> ordinal}; iteration
     *                              order should match the source declaration order so the failure message is readable
     * @param enumName              the simple name of the enum (used in the failure message); pass
     *                              {@code SomeEnum.class.getSimpleName()}
     */
    public static <E extends Enum<E>> void assertOrdinalsMatch(
        E[] actualValues, Map<String, Integer> expectedNameToOrdinal, String enumName) {

        Map<String, Integer> actual = new LinkedHashMap<>();

        for (E value : actualValues) {
            actual.put(value.name(), value.ordinal());
        }

        assertThat(actual)
            .as(
                "%s ordinals must remain stable. The corresponding column stores int ordinal — a reorder, rename, "
                    + "or removal silently re-attributes every historical row. If you need to add a new value, "
                    + "APPEND it at the end and update the expected map in this test. If you need to remove or "
                    + "reorder, first migrate the column to a stable string code.",
                enumName)
            .containsExactlyEntriesOf(expectedNameToOrdinal);
    }
}
