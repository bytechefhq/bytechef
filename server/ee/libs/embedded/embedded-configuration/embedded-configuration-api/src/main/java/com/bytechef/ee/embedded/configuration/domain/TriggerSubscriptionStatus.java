/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

/**
 * Lifecycle status of a connected user trigger subscription, persisted by ordinal. Do not reorder or remove constants;
 * append new ones at the end.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum TriggerSubscriptionStatus {

    ENABLED,
    DISABLED,
    CONNECTION_ERROR,
    FAILED
}
