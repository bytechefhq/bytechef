/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse.schema;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Phase 16 commit 3: builds and validates ClickHouse table names for Context Store per-entity tables.
 *
 * <p>
 * Pattern: {@code cs_w<workspaceId>_c<contextStoreId>_s<sourceId>_<sanitisedEntityName>}. Workspace + store + source
 * IDs prevent cross-tenant and cross-env collision; the entity name is lowercased and filtered to {@code [a-z0-9_]} so
 * dots, dashes, and other punctuation customers might inject through {@code ContextStoreEntity.entityName} can't break
 * out of an identifier and become a SQL injection vector. The result is capped at {@value #MAX_TABLE_NAME_LENGTH}
 * characters — long entity names get truncated AFTER the workspace/store/source prefix so the disambiguation suffix is
 * always preserved.
 * </p>
 *
 * <p>
 * The {@code c<contextStoreId>} segment was added with the multi-env Context Store work; each ContextStore is
 * env-stamped, so this gives DEV/STAGING/PROD physically separate ClickHouse tables for the same logical entity.
 * </p>
 *
 * <p>
 * Returned names are guaranteed to match {@value #VALID_TABLE_NAME_REGEX} which is a strict subset of what ClickHouse
 * accepts as an unquoted identifier (no need for backticks at use-site).
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public final class ClickHouseTableNameSanitizer {

    public static final int MAX_TABLE_NAME_LENGTH = 200;

    public static final String VALID_TABLE_NAME_REGEX = "^cs_w\\d+_c\\d+_s\\d+_[a-z0-9_]+$";

    private static final Pattern ALLOWED_ENTITY_CHAR_PATTERN = Pattern.compile("[^a-z0-9_]");

    private static final Pattern VALID_TABLE_NAME_PATTERN = Pattern.compile(VALID_TABLE_NAME_REGEX);

    private ClickHouseTableNameSanitizer() {
    }

    /**
     * Builds the per-entity ClickHouse table name. Never returns a value that would need quoting at use-site; the regex
     * guarantee on the return value is enforced by {@link #VALID_TABLE_NAME_PATTERN}.
     *
     * @throws IllegalArgumentException if the sanitised entity name is empty (entity names that lower-case to only
     *                                  forbidden characters)
     */
    public static String tableNameFor(long workspaceId, long contextStoreId, long sourceId, String entityName) {
        Objects.requireNonNull(entityName, "entityName");

        String prefix = "cs_w" + workspaceId + "_c" + contextStoreId + "_s" + sourceId + "_";

        String sanitisedEntity = ALLOWED_ENTITY_CHAR_PATTERN.matcher(entityName.toLowerCase(Locale.ROOT))
            .replaceAll("_");

        if (sanitisedEntity.isEmpty() || sanitisedEntity.chars()
            .allMatch(c -> c == '_')) {
            throw new IllegalArgumentException(
                "Entity name '" + entityName + "' reduces to empty after sanitisation; cannot build a ClickHouse "
                    + "table identifier from it.");
        }

        int budget = MAX_TABLE_NAME_LENGTH - prefix.length();

        if (budget <= 0) {
            throw new IllegalArgumentException(
                "workspaceId/contextStoreId/sourceId combination produces a prefix longer than the "
                    + MAX_TABLE_NAME_LENGTH + "-char budget; refusing to truncate the disambiguation suffix.");
        }

        if (sanitisedEntity.length() > budget) {
            sanitisedEntity = sanitisedEntity.substring(0, budget);
        }

        String tableName = prefix + sanitisedEntity;

        if (!VALID_TABLE_NAME_PATTERN.matcher(tableName)
            .matches()) {
            throw new IllegalStateException(
                "Sanitiser produced a name that doesn't match the strict regex — this is a bug: " + tableName);
        }

        return tableName;
    }
}
