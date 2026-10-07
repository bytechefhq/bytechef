/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.constant;

import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class ConnectedUserConstants {

    public static final Set<String> FRONTEND_RESERVED_PATH_SEGMENTS = Set.of(
        "app-events", "automation", "components", "connections", "external",
        "integration-instances", "integrations", "me", "unified", "workflows");

    private ConnectedUserConstants() {
    }
}
