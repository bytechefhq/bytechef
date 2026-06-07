/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

/**
 * Phase 16 follow-up: the four shapes a single {@code indexedFields} edit can take when reconciling against an existing
 * ClickHouse per-entity table. {@link IndexedFieldDiffer} produces a sorted {@code List<IndexedFieldChange>} from the
 * old + new map shapes; {@link ClickHouseTableMigrator} turns each change into the matching {@code ALTER TABLE}
 * statement.
 *
 * <p>
 * Rename is intentionally not a separate shape. A rename presents as {@code Drop(old) + Add(new)}; the JSON
 * {@code _payload} still carries the old field-name → value entry, so old data isn't lost, just no longer projected
 * into a typed column.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface IndexedFieldChange {

    String fieldName();

    /**
     * Field added to {@code indexedFields}. New typed column needed. Existing rows get NULL in the new column until the
     * next sync overwrites them — same lazy-materialisation contract Postgres already has.
     */
    record Add(String fieldName, String fieldType) implements IndexedFieldChange {
    }

    /**
     * Field removed from {@code indexedFields}. Column dropped from the typed projection. The JSON {@code _payload}
     * still carries the key — re-adding the field later re-projects it from any new sync writes.
     */
    record Drop(String fieldName) implements IndexedFieldChange {
    }

    /**
     * Field-type change for an existing field. The migrator attempts {@code MODIFY COLUMN} first; on
     * type-incompatibility it falls back to {@code DROP + ADD} and surfaces the lossy outcome via
     * {@link MigrationResult#lossyTypeChanges()}.
     */
    record TypeChange(String fieldName, String oldFieldType, String newFieldType) implements IndexedFieldChange {
    }
}
