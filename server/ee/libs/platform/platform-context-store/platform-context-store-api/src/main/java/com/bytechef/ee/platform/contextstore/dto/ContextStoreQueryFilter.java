/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.dto;

/**
 * @version ee
 */
public record ContextStoreQueryFilter(String field, FilterOp op, Object value) {

    public enum FilterOp {
        EQ,
        NEQ,
        IN,
        CONTAINS,
        STARTS_WITH,
        GT,
        GTE,
        LT,
        LTE,
        BETWEEN
    }
}
