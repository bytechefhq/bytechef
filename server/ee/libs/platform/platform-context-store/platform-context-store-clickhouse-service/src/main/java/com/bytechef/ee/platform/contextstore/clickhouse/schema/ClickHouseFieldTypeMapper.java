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
 * Phase 16 follow-up: shared field-name validation and field-type → ClickHouse-type mapping used by both
 * {@link ClickHouseTableDdlGenerator} (table creation) and {@link ClickHouseTableMigratorImpl} (in-place schema
 * mutation). Extracted from the DDL generator so the migrator emits identical column types when adding a new column to
 * an existing table.
 *
 * <p>
 * Mapping mirrors the Postgres {@code context_store_record_index} typed-column dispatch in
 * {@code ContextStoreQueryServiceImpl} so query semantics stay consistent across backends:
 * </p>
 *
 * <ul>
 * <li>{@code TEXT} → ClickHouse {@code String}</li>
 * <li>{@code NUMERIC} → ClickHouse {@code Float64}</li>
 * <li>{@code TIMESTAMP} → ClickHouse {@code DateTime64(3)}</li>
 * </ul>
 *
 * <p>
 * Field-name validation enforces the strict {@link #VALID_FIELD_NAME_PATTERN} that keeps SpotBugs's
 * {@code SQL_INJECTION_SPRING_JDBC} taint analysis happy at the use-site — any caller can interpolate a returned field
 * name into a SQL string without quoting.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public final class ClickHouseFieldTypeMapper {

    public static final Pattern VALID_FIELD_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,127}$");

    private ClickHouseFieldTypeMapper() {
    }

    /**
     * Returns the lower-cased field name after validating it against {@link #VALID_FIELD_NAME_PATTERN}. Throws
     * {@link IllegalArgumentException} for dotted paths (e.g. {@code "company.name"}), names starting with digits,
     * names with special characters, or names exceeding 128 characters.
     */
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

    /**
     * Translates a Context Store field-type identifier (TEXT / NUMERIC / TIMESTAMP, case-insensitive) to the matching
     * ClickHouse column type. Throws {@link IllegalArgumentException} for unrecognised types — surfacing the bad input
     * loudly rather than silently defaulting to {@code String}.
     */
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
