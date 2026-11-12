/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscriptionErrorCode;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TriggerSubscriptionEnableException extends RuntimeException {

    private final TriggerSubscriptionErrorCode errorCode;
    private final @Nullable Long triggerSubscriptionId;

    public TriggerSubscriptionEnableException(
        @Nullable Long triggerSubscriptionId, TriggerSubscriptionErrorCode errorCode, String message,
        Throwable cause) {

        super(message, cause);

        this.errorCode = errorCode;
        this.triggerSubscriptionId = triggerSubscriptionId;
    }

    public TriggerSubscriptionErrorCode getErrorCode() {
        return errorCode;
    }

    public @Nullable Long getTriggerSubscriptionId() {
        return triggerSubscriptionId;
    }
}
