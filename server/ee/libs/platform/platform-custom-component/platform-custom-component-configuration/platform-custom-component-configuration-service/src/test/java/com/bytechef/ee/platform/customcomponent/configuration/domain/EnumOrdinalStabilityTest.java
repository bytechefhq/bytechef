/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.domain;

import com.bytechef.test.assertion.OrdinalStabilityAssertions;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the ordinals of enums persisted as INT columns on {@code custom_component}. Append new values at the end only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testCustomComponentStatusOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("DRAFT", 0);
        expected.put("PUBLISHED", 1);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            CustomComponent.Status.values(), expected, "CustomComponent.Status");
    }

    @Test
    void testCustomComponentLanguageOrdinals() {
        Map<String, Integer> expected = new LinkedHashMap<>();

        expected.put("JAVA", 0);
        expected.put("JAVASCRIPT", 1);
        expected.put("PYTHON", 2);
        expected.put("RUBY", 3);

        OrdinalStabilityAssertions.assertOrdinalsMatch(
            CustomComponent.Language.values(), expected, "CustomComponent.Language");
    }
}
