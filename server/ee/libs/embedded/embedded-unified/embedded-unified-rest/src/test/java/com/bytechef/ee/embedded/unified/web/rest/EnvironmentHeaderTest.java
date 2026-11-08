/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnvironmentHeaderTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "development", "Development", "DEVELOPMENT", " development "
    })
    void testToEnvironmentReadsAnyLetterCase(String environment) {
        assertEquals(Environment.DEVELOPMENT, EnvironmentHeader.toEnvironment(environment));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "", "   "
    })
    void testToEnvironmentDefaultsAMissingOrBlankHeaderToProduction(String environment) {
        assertEquals(Environment.PRODUCTION, EnvironmentHeader.toEnvironment(environment));
    }

    @Test
    void testToEnvironmentRejectsAnUnknownName() {
        assertThrows(IllegalArgumentException.class, () -> EnvironmentHeader.toEnvironment("test"));
    }
}
