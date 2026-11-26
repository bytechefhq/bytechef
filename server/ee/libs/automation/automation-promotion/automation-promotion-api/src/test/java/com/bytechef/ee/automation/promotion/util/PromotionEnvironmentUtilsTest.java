/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PromotionEnvironmentUtilsTest {

    @Test
    void testDisplayName() {
        assertThat(PromotionEnvironmentUtils.displayName(Environment.DEVELOPMENT)).isEqualTo("Development");
        assertThat(PromotionEnvironmentUtils.displayName(Environment.STAGING)).isEqualTo("Staging");
        assertThat(PromotionEnvironmentUtils.displayName(Environment.PRODUCTION)).isEqualTo("Production");
    }
}
