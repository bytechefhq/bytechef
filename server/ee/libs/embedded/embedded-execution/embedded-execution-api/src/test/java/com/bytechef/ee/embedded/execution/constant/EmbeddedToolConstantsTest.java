/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.configuration.domain.Environment;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class EmbeddedToolConstantsTest {

    @Test
    void testWithConnectedUserContextAddsReservedKeysAndPreservesInputs() {
        Map<String, Object> result = EmbeddedToolConstants.withConnectedUserContext(
            Map.of("prompt", "build a thing"), "user-1", Environment.PRODUCTION);

        assertEquals("user-1", result.get(EmbeddedToolConstants.EXTERNAL_USER_ID));
        assertEquals("PRODUCTION", result.get(EmbeddedToolConstants.ENVIRONMENT));
        assertEquals("build a thing", result.get("prompt"));
    }

    @Test
    void testWithConnectedUserContextDoesNotMutateInput() {
        Map<String, Object> input = Map.of("prompt", "x");

        EmbeddedToolConstants.withConnectedUserContext(input, "user-1", Environment.STAGING);

        assertEquals(1, input.size());
    }
}
