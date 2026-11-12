/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

/**
 * Reason a trigger subscription failed, persisted by name.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum TriggerSubscriptionErrorCode {

    USER_INPUT_ERROR,
    API_ERROR,
    TIMEOUT_ERROR,
    OAUTH_REFRESH,
    NO_TRIGGER,
    INTERNAL_ERROR
}
