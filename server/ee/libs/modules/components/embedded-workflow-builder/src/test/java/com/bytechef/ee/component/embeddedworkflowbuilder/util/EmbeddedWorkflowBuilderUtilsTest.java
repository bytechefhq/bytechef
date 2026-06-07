/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.platform.configuration.domain.Environment;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class EmbeddedWorkflowBuilderUtilsTest {

    @Test
    void testResolveEnvironmentParsesName() {
        assertEquals(Environment.STAGING, EmbeddedWorkflowBuilderUtils.resolveEnvironment("STAGING"));
    }

    @Test
    void testResolveEnvironmentDefaultsToProductionWhenBlank() {
        assertEquals(Environment.PRODUCTION, EmbeddedWorkflowBuilderUtils.resolveEnvironment(null));
        assertEquals(Environment.PRODUCTION, EmbeddedWorkflowBuilderUtils.resolveEnvironment(" "));
    }
}
