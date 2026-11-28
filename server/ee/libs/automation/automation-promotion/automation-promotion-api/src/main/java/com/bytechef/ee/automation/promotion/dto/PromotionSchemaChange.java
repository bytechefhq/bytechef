/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.dto;

import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record PromotionSchemaChange(
    String tableName, @Nullable String columnName, Kind kind, @Nullable String fromType, @Nullable String toType,
    boolean destructive) {

    public enum Kind {
        CREATE_TABLE, ADD_COLUMN, DROP_COLUMN, CHANGE_COLUMN_TYPE, CHANGE_LINK
    }
}
