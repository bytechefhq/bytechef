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
public record MigrationResult(int adds, int drops, int compatibleTypeChanges, int lossyTypeChanges) {

    public static final MigrationResult NONE = new MigrationResult(0, 0, 0, 0);

    public int total() {
        return adds + drops + compatibleTypeChanges + lossyTypeChanges;
    }
}
