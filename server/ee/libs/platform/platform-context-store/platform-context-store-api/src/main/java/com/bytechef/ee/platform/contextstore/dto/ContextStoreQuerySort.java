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
public record ContextStoreQuerySort(String field, SortDirection dir) {

    public enum SortDirection {
        ASC,
        DESC
    }
}
