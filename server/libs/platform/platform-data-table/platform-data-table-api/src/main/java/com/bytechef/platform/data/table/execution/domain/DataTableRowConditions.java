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

package com.bytechef.platform.data.table.execution.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * OR groups of AND conditions: a row matches when it satisfies every filter of at least one group. No groups, as built
 * by {@link #matchAll()} or by the constructor with an empty list, match every row; an empty group is rejected.
 * {@link #of(List)} needs at least one filter.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record DataTableRowConditions(List<List<DataTableRowFilter>> groups) {

    private static final DataTableRowConditions MATCH_ALL = new DataTableRowConditions(List.of());

    public DataTableRowConditions {
        Objects.requireNonNull(groups, "groups must not be null");

        groups = groups.stream()
            .map(group -> List.copyOf(Objects.requireNonNull(group, "condition group must not be null")))
            .toList();

        for (int index = 0; index < groups.size(); index++) {
            List<DataTableRowFilter> group = groups.get(index);

            if (group.isEmpty()) {
                throw new IllegalArgumentException("Condition group %d is empty".formatted(index + 1));
            }
        }
    }

    public static DataTableRowConditions matchAll() {
        return MATCH_ALL;
    }

    public static DataTableRowConditions of(List<DataTableRowFilter> filters) {
        Objects.requireNonNull(filters, "filters must not be null");

        if (filters.isEmpty()) {
            throw new IllegalArgumentException("At least one filter is required; use matchAll() to match every row");
        }

        return new DataTableRowConditions(List.of(filters));
    }

    public boolean isMatchAll() {
        return groups.isEmpty();
    }
}
