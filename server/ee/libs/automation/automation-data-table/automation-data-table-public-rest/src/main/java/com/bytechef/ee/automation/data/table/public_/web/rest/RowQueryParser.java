/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.domain.RowSort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Turns the wire filter and sort grammar into the platform's {@link RowFilter}/{@link RowSort}. Values are never
 * interpreted here -- type coercion is the service's -- so the only failures are shape, operator and column.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class RowQueryParser {

    private RowQueryParser() {
    }

    static List<RowFilter> parseFilters(@Nullable List<String> raw, Set<String> filterableColumns) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<RowFilter> rowFilters = new ArrayList<>(raw.size());

        for (String term : raw) {
            String[] parts = term.split(":", 3);

            if (parts.length < 3) {
                throw new DataTableException(
                    "A filter is column:OPERATOR:value, got '" + term + "'", DataTableErrorType.FILTER_INVALID);
            }

            String column = column(parts[0], filterableColumns, DataTableErrorType.FILTER_INVALID);
            RowFilter.Operator operator = operator(parts[1], term);
            String rawValue = parts[2];

            Object value = switch (operator) {
                case IN, BETWEEN -> {
                    List<String> values = Arrays.stream(rawValue.split(","))
                        .map(String::trim)
                        .filter(item -> !item.isEmpty())
                        .toList();

                    if (operator == RowFilter.Operator.BETWEEN && values.size() != 2) {
                        throw new DataTableException(
                            "BETWEEN takes exactly two comma-separated values, got '" + rawValue + "'",
                            DataTableErrorType.FILTER_INVALID);
                    }

                    if (values.isEmpty()) {
                        throw new DataTableException(
                            "IN needs at least one value, got '" + rawValue + "'", DataTableErrorType.FILTER_INVALID);
                    }

                    yield values;
                }
                default -> rawValue;
            };

            rowFilters.add(new RowFilter(column, operator, value));
        }

        return rowFilters;
    }

    static List<RowSort> parseSorts(@Nullable List<String> raw, Set<String> sortableColumns) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<RowSort> rowSorts = new ArrayList<>(raw.size());

        for (String term : raw) {
            String[] parts = term.split(":", 2);

            if (parts.length != 2) {
                throw new DataTableException(
                    "A sort is column:ASC or column:DESC, got '" + term + "'", DataTableErrorType.SORT_INVALID);
            }

            String column = column(parts[0], sortableColumns, DataTableErrorType.SORT_INVALID);

            RowSort.Direction direction;

            try {
                direction = RowSort.Direction.valueOf(parts[1].trim()
                    .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new DataTableException(
                    "Unknown sort direction '" + parts[1] + "'", DataTableErrorType.SORT_INVALID);
            }

            rowSorts.add(new RowSort(column, direction));
        }

        return rowSorts;
    }

    private static String column(String field, Set<String> columns, DataTableErrorType errorType) {
        String column = field.trim()
            .toLowerCase(Locale.ROOT);

        if ("externalid".equals(column)) {
            column = ReservedColumns.EXTERNAL_ID;
        }

        if (column.isEmpty() || !columns.contains(column)) {
            throw new DataTableException("Unknown column '" + field + "'", errorType);
        }

        return column;
    }

    private static RowFilter.Operator operator(String raw, String term) {
        try {
            return RowFilter.Operator.valueOf(raw.trim()
                .toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new DataTableException(
                "Unknown operator in '" + term + "'", DataTableErrorType.FILTER_INVALID);
        }
    }
}
