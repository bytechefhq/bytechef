/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.exception;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.exception.AbstractErrorType;

/**
 * Structured error keys for {@link ContextStoreSource} failures surfaced via GraphQL. Used with
 * {@link com.bytechef.exception.ConfigurationException} so the global resolver maps them to {@code BAD_REQUEST} and
 * forwards the message verbatim (instead of the default sanitized {@code INTERNAL_ERROR for <uuid>} response).
 *
 * @author Ivica Cardic
 * @version ee
 */
public class ContextStoreSourceErrorType extends AbstractErrorType {

    public static final ContextStoreSourceErrorType MISSING_WORKFLOW = new ContextStoreSourceErrorType(100);
    public static final ContextStoreSourceErrorType REFRESH_FAILED = new ContextStoreSourceErrorType(101);
    public static final ContextStoreSourceErrorType MISSING_REQUIRED_PARAMETER = new ContextStoreSourceErrorType(102);

    private ContextStoreSourceErrorType(int errorKey) {
        super(ContextStoreSource.class, errorKey);
    }
}
