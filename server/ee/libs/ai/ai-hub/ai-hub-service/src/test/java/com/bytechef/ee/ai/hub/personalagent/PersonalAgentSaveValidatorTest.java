/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the save-time behavior of {@link PersonalAgentSaveValidator#validate}. Gateway-based validation is disabled for
 * now, so {@code validate} is a no-op for every input — catalog selections (provider keys like
 * {@code "ai.provider.openAi"}) were always resolved at runtime, and gateway type names no longer raise at save.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class PersonalAgentSaveValidatorTest {

    private static final long WORKSPACE_ID = 42L;
    private static final String OPENAI_MODEL = "gpt-4o";

    private PersonalAgentSaveValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PersonalAgentSaveValidator();
    }

    @Test
    void testCatalogProviderKeyDoesNotThrow() {
        assertDoesNotThrow(() -> validator.validate(WORKSPACE_ID, "ai.provider.openAi", OPENAI_MODEL));
    }

    @Test
    void testGatewayProviderTypeDoesNotThrow() {
        // Gateway-based provider/model validation is disabled for now, so even a name the workspace doesn't recognize
        // no longer raises at save — the runtime resolver's warn-and-fallback is the only remaining check.
        assertDoesNotThrow(() -> validator.validate(WORKSPACE_ID, "GROQ", "llama-3"));
    }
}
