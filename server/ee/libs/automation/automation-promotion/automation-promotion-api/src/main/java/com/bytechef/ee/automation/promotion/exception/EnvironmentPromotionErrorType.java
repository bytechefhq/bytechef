/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.exception;

import com.bytechef.exception.AbstractErrorType;

/**
 * Structured error keys for environment-promotion failures, used with
 * {@link com.bytechef.exception.ConfigurationException} so the global resolver maps them to {@code BAD_REQUEST} and
 * forwards the message verbatim (instead of the default sanitized {@code INTERNAL_ERROR for <uuid>} response).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EnvironmentPromotionErrorType extends AbstractErrorType {

    public static final EnvironmentPromotionErrorType SAME_ENVIRONMENT = new EnvironmentPromotionErrorType(100);
    public static final EnvironmentPromotionErrorType ENVIRONMENT_NOT_AVAILABLE =
        new EnvironmentPromotionErrorType(101);
    public static final EnvironmentPromotionErrorType SOURCE_NOT_FOUND = new EnvironmentPromotionErrorType(102);
    public static final EnvironmentPromotionErrorType TARGET_CONNECTION_INVALID =
        new EnvironmentPromotionErrorType(103);
    public static final EnvironmentPromotionErrorType TARGET_NAME_CONFLICT = new EnvironmentPromotionErrorType(104);
    public static final EnvironmentPromotionErrorType UNSUPPORTED_RESOURCE_TYPE =
        new EnvironmentPromotionErrorType(105);
    public static final EnvironmentPromotionErrorType SYNTHETIC_DEPLOYMENT_NOT_PROMOTABLE =
        new EnvironmentPromotionErrorType(106);

    private EnvironmentPromotionErrorType(int errorKey) {
        super(EnvironmentPromotionErrorType.class, errorKey);
    }
}
