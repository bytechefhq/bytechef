/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

/**
 * Why ByteChef disabled or deleted a trigger subscription on its own, sent as {@code reason} in the
 * {@code subscription.disabled} and {@code subscription.deleted} events. {@code CONNECTION_DELETED} and
 * {@code CONNECTED_USER_DELETED} describe deletions; the other reasons describe disables.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum TriggerSubscriptionDisableReason {

    CONNECTION_DELETED,
    CONNECTION_INVALID,
    INTEGRATION_INSTANCE_DISABLED,
    INTEGRATION_INSTANCE_DELETED,
    CONNECTED_USER_DISABLED,
    CONNECTED_USER_DELETED
}
