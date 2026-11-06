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

public final class OrdinalStabilityAssertions {

    private OrdinalStabilityAssertions() {
    }

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
