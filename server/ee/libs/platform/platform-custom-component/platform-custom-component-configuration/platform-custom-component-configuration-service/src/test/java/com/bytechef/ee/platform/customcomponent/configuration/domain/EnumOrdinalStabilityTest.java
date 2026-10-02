/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalStabilityTest {

    @Test
    void testCustomComponentStatusOrdinals() {
        assertEquals(0, CustomComponent.Status.DRAFT.ordinal());
        assertEquals(1, CustomComponent.Status.PUBLISHED.ordinal());
        assertEquals(2, CustomComponent.Status.values().length);
    }

    @Test
    void testCustomComponentLanguageOrdinals() {
        assertEquals(0, CustomComponent.Language.JAVA.ordinal());
        assertEquals(1, CustomComponent.Language.JAVASCRIPT.ordinal());
        assertEquals(2, CustomComponent.Language.PYTHON.ordinal());
        assertEquals(3, CustomComponent.Language.RUBY.ordinal());
        assertEquals(4, CustomComponent.Language.values().length);
    }
}
