/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.component.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentOperationPolicyOperationTypeTest {

    @Test
    void testOperationTypeOrdinalsAreStable() {
        assertThat(ComponentOperationPolicy.OperationType.ACTION.ordinal()).isEqualTo(0);
        assertThat(ComponentOperationPolicy.OperationType.TRIGGER.ordinal()).isEqualTo(1);
    }

    @Test
    void testKeyFormat() {
        assertThat(
            ComponentOperationPolicy.key("slack", ComponentOperationPolicy.OperationType.ACTION, "sendMessage"))
                .isEqualTo("slack#ACTION#sendMessage");
    }
}
