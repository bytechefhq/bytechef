/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.domain;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A named string value defined once per {@link VariableScope} and environment, referenced from workflows as
 * {@code ${vars.<name>}}. Backed by one {@code property} row whose key is {@link #KEY_PREFIX} + name.
 *
 * @version ee
 */
public record Variable(
    long id, String name, String value, int environmentId, @Nullable String createdBy, @Nullable Instant createdDate,
    @Nullable String lastModifiedBy, @Nullable Instant lastModifiedDate) {

    public static final String KEY_PREFIX = "variable.";
    public static final String VALUE_KEY = "value";
}
