/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class ScimPagination {

    static final int MAX_RESULTS = 200;

    private ScimPagination() {
    }

    static int normalizeStartIndex(int startIndex) {
        return Math.max(1, startIndex);
    }

    static int normalizeCount(int count) {
        return Math.clamp(count, 0, MAX_RESULTS);
    }
}
