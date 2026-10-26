/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.util;

import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class EnumOrdinalStabilityTest {

    @Test
    void testContextStoreSourceStatusOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("BUILDING_PREVIEW", 0);
        expected.put("PREVIEW", 1);
        expected.put("READY", 2);
        expected.put("FAILED", 3);
        expected.put("DISABLED", 4);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            ContextStoreSourceStatus.values(), expected, ContextStoreSourceStatus.class.getSimpleName());
    }

    @Test
    void testTombstoneStrategyOrdinalsAreStable() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("PERIODIC_FULL_REPLACE", 0);
        expected.put("UPSTREAM_CHANGE_FEED", 1);
        expected.put("NONE", 2);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            TombstoneStrategy.values(), expected, TombstoneStrategy.class.getSimpleName());
    }

}
