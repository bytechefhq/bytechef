/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

/**
 * @author Ivica Cardic
 * @version ee
 */
public sealed interface IndexedFieldChange {

    String fieldName();

    record Add(String fieldName, String fieldType) implements IndexedFieldChange {
    }

    record Drop(String fieldName) implements IndexedFieldChange {
    }

    record TypeChange(String fieldName, String oldFieldType, String newFieldType) implements IndexedFieldChange {
    }
}
