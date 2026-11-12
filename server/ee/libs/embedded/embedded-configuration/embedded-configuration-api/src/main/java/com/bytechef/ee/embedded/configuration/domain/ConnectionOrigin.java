/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

/**
 * How a connected user came to have a connection: an admin connection shared with every connected user, one the
 * connected user created, or one an admin created for the connected user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ConnectionOrigin {

    INTERNAL,
    PROVISIONED,
    SELF_SERVE
}
