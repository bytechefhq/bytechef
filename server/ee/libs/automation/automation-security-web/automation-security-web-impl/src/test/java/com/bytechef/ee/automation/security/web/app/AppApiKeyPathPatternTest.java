/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.security.web.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AppApiKeyPathPatternTest {

    @Test
    void testActionsListingIsOnTheAppKeyChain() {
        assertThat("/api/app/v1/actions").matches(AppApiKeySecurityConfigurer.PATH_PATTERN);
    }
}
