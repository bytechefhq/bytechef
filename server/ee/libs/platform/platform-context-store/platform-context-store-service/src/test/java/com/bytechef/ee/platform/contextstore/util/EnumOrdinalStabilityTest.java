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
 * Pins the ordinal of every persisted enum value used by the Context Store.
 * {@link com.bytechef.ee.platform.contextstore.domain.ContextStoreSource} stores {@link ContextStoreSourceStatus} as an
 * {@code int} ordinal column. A reorder, rename, or removal silently re-attributes every historical row to a different
 * value at the same ordinal — there is no DB-side guard.
 *
 * <p>
 * <strong>This test fails on any reorder, rename, or removal.</strong> When you legitimately need to add a new value,
 * <em>append it at the end</em> and add a new entry to the corresponding map below.
 * </p>
 *
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
