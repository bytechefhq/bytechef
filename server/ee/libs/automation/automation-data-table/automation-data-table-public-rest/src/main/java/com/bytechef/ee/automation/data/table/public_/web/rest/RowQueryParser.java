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
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class RowQueryParser {

    private static final Map<String, Operation> OPERATION_ALIASES = Map.of(
        "EQ", Operation.EQUALS,
        "NEQ", Operation.NOT_EQUALS,
        "GT", Operation.GREATER,
        "GTE", Operation.GREATER_EQUALS,
        "LT", Operation.LESS,
        "LTE", Operation.LESS_EQUALS);

    private RowQueryParser() {
    }

    static DataTableRowConditions parseConditions(@Nullable List<String> rawFilters, Set<String> filterableColumns) {
        List<DataTableRowFilter> dataTableRowFilters = parseFilters(rawFilters, filterableColumns);

        return dataTableRowFilters.isEmpty()
            ? DataTableRowConditions.matchAll() : DataTableRowConditions.of(dataTableRowFilters);
    }

    static List<DataTableRowFilter> parseFilters(@Nullable List<String> rawFilters, Set<String> filterableColumns) {
        if (rawFilters == null || rawFilters.isEmpty()) {
            return List.of();
        }

        List<DataTableRowFilter> dataTableRowFilters = new ArrayList<>(rawFilters.size());

        for (String term : rawFilters) {
            String[] parts = term.split(":", 3);

            if (parts.length < 2) {
                throw new DataTableException(
                    "A filter is column:OPERATOR:value, got '" + term + "'", DataTableErrorType.FILTER_INVALID);
            }

            String column = column(parts[0], filterableColumns, DataTableErrorType.FILTER_INVALID);
            Operation operation = operation(parts[1], term);

            if (operation.arity() != Operation.Arity.NONE && parts.length < 3) {
                throw new DataTableException(
                    "A filter is column:OPERATOR:value, got '" + term + "'", DataTableErrorType.FILTER_INVALID);
            }

            Object value = switch (operation.arity()) {
                case NONE -> null;
                case ONE -> parts[2];
                case LIST, PAIR -> listValue(operation, parts[2]);
            };

            try {
                dataTableRowFilters.add(new DataTableRowFilter(column, operation, value));
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new DataTableException(
                    illegalArgumentException.getMessage(), DataTableErrorType.FILTER_INVALID);
            }
        }

        return dataTableRowFilters;
    }

    static List<DataTableRowSort> parseSorts(@Nullable List<String> rawSorts, Set<String> sortableColumns) {
        if (rawSorts == null || rawSorts.isEmpty()) {
            return List.of();
        }

        List<DataTableRowSort> dataTableRowSorts = new ArrayList<>(rawSorts.size());

        for (String term : rawSorts) {
            String[] parts = term.split(":", 2);

            if (parts.length != 2) {
                throw new DataTableException(
                    "A sort is column:ASC or column:DESC, got '" + term + "'", DataTableErrorType.SORT_INVALID);
            }

            String column = column(parts[0], sortableColumns, DataTableErrorType.SORT_INVALID);

            String directionName = parts[1].trim()
                .toUpperCase(Locale.ROOT);

            Direction direction;

            try {
                direction = Direction.valueOf(directionName);
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new DataTableException(
                    "Unknown sort direction '" + parts[1] + "'", DataTableErrorType.SORT_INVALID);
            }

            dataTableRowSorts.add(new DataTableRowSort(column, direction));
        }

        return dataTableRowSorts;
    }

    private static String column(String field, Set<String> columns, DataTableErrorType errorType) {
        String trimmedField = field.trim();

        String column = trimmedField.toLowerCase(Locale.ROOT);

        if ("externalid".equals(column)) {
            column = ReservedColumns.EXTERNAL_ID;
        }

        if (column.isEmpty() || !columns.contains(column)) {
            throw new DataTableException("Unknown column '" + field + "'", errorType);
        }

        return column;
    }

    private static List<String> listValue(Operation operation, String rawValue) {
        List<String> values = Arrays.stream(rawValue.split(","))
            .map(String::trim)
            .filter(item -> !item.isEmpty())
            .toList();

        if (operation == Operation.BETWEEN && values.size() != 2) {
            throw new DataTableException(
                "BETWEEN takes exactly two comma-separated values, got '" + rawValue + "'",
                DataTableErrorType.FILTER_INVALID);
        }

        if (values.isEmpty()) {
            throw new DataTableException(
                "IN needs at least one value, got '" + rawValue + "'", DataTableErrorType.FILTER_INVALID);
        }

        return values;
    }

    private static Operation operation(String rawOperation, String term) {
        String trimmedOperation = rawOperation.trim();

        String operationName = trimmedOperation.toUpperCase(Locale.ROOT);

        Operation aliasedOperation = OPERATION_ALIASES.get(operationName);

        if (aliasedOperation != null) {
            return aliasedOperation;
        }

        try {
            return Operation.valueOf(operationName);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new DataTableException("Unknown operator in '" + term + "'", DataTableErrorType.FILTER_INVALID);
        }
    }
}
