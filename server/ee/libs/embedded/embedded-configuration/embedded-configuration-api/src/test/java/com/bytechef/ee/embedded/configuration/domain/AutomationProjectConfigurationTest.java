/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AutomationProjectConfigurationTest {

    @Test
    void testNoArgConstructorAssignsNoUuid() {
        AutomationProjectConfiguration automationProjectConfiguration = new AutomationProjectConfiguration();

        assertThat(automationProjectConfiguration.getUuid()).isNull();
    }

    @Test
    void testBusinessConstructorAssignsUuid() {
        AutomationProjectConfiguration automationProjectConfiguration = new AutomationProjectConfiguration(
            1L, 0L, "configuration", null, 1, false, false);

        assertThat(automationProjectConfiguration.getUuid()).isNotNull();
    }

    @Test
    void testSetUuidRejectsNull() {
        AutomationProjectConfiguration automationProjectConfiguration = new AutomationProjectConfiguration();

        assertThatThrownBy(() -> automationProjectConfiguration.setUuid(null))
            .isInstanceOf(NullPointerException.class);
    }
}
