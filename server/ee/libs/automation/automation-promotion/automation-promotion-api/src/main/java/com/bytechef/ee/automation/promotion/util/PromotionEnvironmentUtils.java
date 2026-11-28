/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.util;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Locale;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class PromotionEnvironmentUtils {

    private PromotionEnvironmentUtils() {
    }

    public static String displayName(Environment environment) {
        String name = environment.name();

        String remainder = name.substring(1);

        return name.charAt(0) + remainder.toLowerCase(Locale.ROOT);
    }
}
