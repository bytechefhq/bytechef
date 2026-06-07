/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

/**
 * Phase 16 follow-up: per-change counts returned by {@link ClickHouseTableMigrator#applyChanges}. Callers consume this
 * to drive metrics and to surface "you had a lossy type change, the typed projection was reset" to the user.
 *
 * <p>
 * {@code lossyTypeChanges} is separated from {@code compatibleTypeChanges} because the lossy path goes through
 * {@code DROP COLUMN} + {@code ADD COLUMN} — the column's typed projection resets to NULL for all existing rows (the
 * JSON {@code _payload} preserves the values; only the typed column loses them). The compatible path uses
 * {@code MODIFY COLUMN} and retains values.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public record MigrationResult(int adds, int drops, int compatibleTypeChanges, int lossyTypeChanges) {

    public static final MigrationResult NONE = new MigrationResult(0, 0, 0, 0);

    public int total() {
        return adds + drops + compatibleTypeChanges + lossyTypeChanges;
    }
}
