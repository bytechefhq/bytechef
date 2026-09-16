/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Strict request validation, deliberately stricter than the service's {@code coerceValue}: that path turns an
 * unparseable INTEGER into null for the workflow editor's benefit, which on a public API would be a silent data change.
 * Unknown columns are rejected here for the same reason.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class RowValuesValidator {

    private static final int MAX_EXTERNAL_ID_LENGTH = 255;

    private RowValuesValidator() {
    }

    static void validate(Map<String, Object> values, List<ColumnSpec> columnSpecs) {
        Map<String, ColumnType> columnTypes = new HashMap<>();

        for (ColumnSpec columnSpec : columnSpecs) {
            columnTypes.put(columnSpec.name()
                .toLowerCase(Locale.ROOT), columnSpec.type());
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String column = entry.getKey()
                .toLowerCase(Locale.ROOT);
            ColumnType columnType = columnTypes.get(column);

            if (columnType == null || ReservedColumns.isReserved(column)) {
                throw new DataTableException(
                    "'" + entry.getKey() + "' is not a column of this table", DataTableErrorType.ROW_VALUE_INVALID);
            }

            Object value = entry.getValue();

            if (value == null || isCoercible(columnType, value)) {
                continue;
            }

            throw new DataTableException(
                "Value '" + value + "' is not a valid " + columnType + " for column '" + entry.getKey() + "'",
                DataTableErrorType.ROW_VALUE_INVALID);
        }
    }

    static void validateExternalId(@Nullable String externalId) {
        if (externalId == null || externalId.isBlank() || externalId.length() > MAX_EXTERNAL_ID_LENGTH) {
            throw new DataTableException(
                "externalId must be 1-" + MAX_EXTERNAL_ID_LENGTH + " characters",
                DataTableErrorType.ROW_VALUE_INVALID);
        }
    }

    private static boolean isCoercible(ColumnType columnType, Object value) {
        String text = String.valueOf(value)
            .trim();

        try {
            return switch (columnType) {
                case STRING -> true;
                case BOOLEAN -> value instanceof Boolean || "true".equalsIgnoreCase(text) ||
                    "false".equalsIgnoreCase(text);
                case INTEGER -> value instanceof Integer || value instanceof Long || parsesAsLong(text);
                case NUMBER -> value instanceof Number || parsesAsDecimal(text);
                case DATE -> parsesAsDate(text);
                case DATE_TIME -> parsesAsDateTime(text);
            };
        } catch (RuntimeException runtimeException) {
            return false;
        }
    }

    private static boolean parsesAsLong(String text) {
        Long.parseLong(text);

        return true;
    }

    private static boolean parsesAsDecimal(String text) {
        new BigDecimal(text);

        return true;
    }

    private static boolean parsesAsDate(String text) {
        LocalDate.parse(text);

        return true;
    }

    private static boolean parsesAsDateTime(String text) {
        try {
            OffsetDateTime.parse(text);
        } catch (DateTimeParseException dateTimeParseException) {
            LocalDateTime.parse(text);
        }

        return true;
    }
}
