/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppLogValuesTest {

    @Test
    void testToLoggableValueReplacesLineBreaksAndControlCharacters() {
        assertThat(AppLogValues.toLoggableValue("PENDING\r\nINFO forged entry\u0007\u2028x", 64))
            .isEqualTo("PENDING  INFO forged entry  x");
    }

    @Test
    void testToLoggableValueAbbreviatesALongValue() {
        assertThat(AppLogValues.toLoggableValue("A".repeat(100), 10)).isEqualTo("AAAAAAA...");
    }

    @Test
    void testToLoggableValueKeepsAPlainValue() {
        assertThat(AppLogValues.toLoggableValue("PENDING", 64)).isEqualTo("PENDING");
    }
}
