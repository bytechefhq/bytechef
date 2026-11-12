/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.exception;

import com.bytechef.ee.embedded.configuration.domain.TriggerSubscription;
import com.bytechef.exception.AbstractErrorType;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class TriggerSubscriptionErrorType extends AbstractErrorType {

    public static final TriggerSubscriptionErrorType NO_TRIGGER = new TriggerSubscriptionErrorType(100);
    public static final TriggerSubscriptionErrorType TRIGGER_SUBSCRIPTION_NOT_FOUND =
        new TriggerSubscriptionErrorType(101);
    public static final TriggerSubscriptionErrorType USER_INPUT_ERROR = new TriggerSubscriptionErrorType(102);

    private TriggerSubscriptionErrorType(int errorKey) {
        super(TriggerSubscription.class, errorKey);
    }
}
