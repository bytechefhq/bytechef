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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * OR groups of AND conditions: a row matches when it satisfies every filter of at least one group. Only
 * {@link #matchAll()} matches every row; {@link #of(List)} and {@link #anyOf(List)} need at least one filter and reject
 * an empty group.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class DataTableRowConditions {

    private static final DataTableRowConditions MATCH_ALL = new DataTableRowConditions(List.of());

    private final List<List<DataTableRowFilter>> groups;

    private DataTableRowConditions(List<List<DataTableRowFilter>> groups) {
        this.groups = groups;
    }

    public static DataTableRowConditions anyOf(List<List<DataTableRowFilter>> groups) {
        Objects.requireNonNull(groups, "groups must not be null");

        if (groups.isEmpty()) {
            throw new IllegalArgumentException(
                "At least one condition group is required; use matchAll() to match every row");
        }

        List<List<DataTableRowFilter>> copiedGroups = new ArrayList<>();

        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            List<DataTableRowFilter> group = Objects.requireNonNull(
                groups.get(groupIndex), "condition group %d must not be null".formatted(groupIndex + 1));

            if (group.isEmpty()) {
                throw new IllegalArgumentException("Condition group %d is empty".formatted(groupIndex + 1));
            }

            for (int filterIndex = 0; filterIndex < group.size(); filterIndex++) {
                Objects.requireNonNull(
                    group.get(filterIndex),
                    "filter %d of condition group %d must not be null".formatted(filterIndex + 1, groupIndex + 1));
            }

            copiedGroups.add(List.copyOf(group));
        }

        return new DataTableRowConditions(List.copyOf(copiedGroups));
    }

    public static DataTableRowConditions matchAll() {
        return MATCH_ALL;
    }

    public static DataTableRowConditions of(List<DataTableRowFilter> filters) {
        Objects.requireNonNull(filters, "filters must not be null");

        if (filters.isEmpty()) {
            throw new IllegalArgumentException("At least one filter is required; use matchAll() to match every row");
        }

        return anyOf(List.of(filters));
    }

    public List<List<DataTableRowFilter>> groups() {
        return groups;
    }

    public boolean isMatchAll() {
        return groups.isEmpty();
    }

    /**
     * Returns these conditions when they hold at least one condition, for operations that change the selected rows.
     *
     * @return these conditions
     * @throws IllegalArgumentException if these conditions match every row
     */
    public DataTableRowConditions requireAtLeastOneCondition() {
        if (isMatchAll()) {
            throw new IllegalArgumentException("At least one condition is required to select the rows to change");
        }

        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        return object instanceof DataTableRowConditions dataTableRowConditions
            && groups.equals(dataTableRowConditions.groups);
    }

    @Override
    public int hashCode() {
        return groups.hashCode();
    }

    @Override
    public String toString() {
        return "DataTableRowConditions{groups=" + groups + "}";
    }
}
