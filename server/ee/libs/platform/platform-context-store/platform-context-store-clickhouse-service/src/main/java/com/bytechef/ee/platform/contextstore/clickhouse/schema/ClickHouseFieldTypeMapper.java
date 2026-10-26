/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class ClickHouseFieldTypeMapper {

    public static final Pattern VALID_FIELD_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,127}$");

    private ClickHouseFieldTypeMapper() {
    }

    public static String validateFieldName(String fieldName) {
        String lowercased = fieldName.toLowerCase(Locale.ROOT);

        if (!VALID_FIELD_NAME_PATTERN.matcher(lowercased)
            .matches()) {
            throw new IllegalArgumentException(
                "Indexed field name '" + fieldName
                    + "' is not safe for use as a ClickHouse column identifier (must match "
                    + VALID_FIELD_NAME_PATTERN.pattern()
                    + " after lowercasing; dotted paths and special characters are not supported in Phase 16).");
        }

        return lowercased;
    }

    public static String toClickHouseType(String fieldType) {
        String normalised = fieldType.toUpperCase(Locale.ROOT);

        return switch (normalised) {
            case "TEXT" -> "String";
            case "NUMERIC" -> "Float64";
            case "TIMESTAMP" -> "DateTime64(3)";
            default -> throw new IllegalArgumentException(
                "Unsupported indexed-field type '" + fieldType + "'; expected one of TEXT, NUMERIC, TIMESTAMP.");
        };
    }
}
