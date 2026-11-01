/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.util;

import com.bytechef.platform.constant.PlatformType;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AuditPlatformUtils {

    private AuditPlatformUtils() {
    }

    public static @Nullable PlatformType resolvePlatform(@Nullable String className) {
        if (className == null) {
            return null;
        }

        if (className.contains(".embedded.")) {
            return PlatformType.EMBEDDED;
        }

        if (className.contains(".automation.")) {
            return PlatformType.AUTOMATION;
        }

        return null;
    }
}
