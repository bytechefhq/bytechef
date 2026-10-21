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
class AppBindingKeysTest {

    @Test
    void testToFlatKeyCollapsesSeparatorsAndUpperCases() {
        assertThat(AppBindingKeys.toFlatKey("support-crm")).isEqualTo("SUPPORT_CRM");
        assertThat(AppBindingKeys.toFlatKey("support_crm")).isEqualTo("SUPPORT_CRM");
        assertThat(AppBindingKeys.toFlatKey("a--b")).isEqualTo("A_B");
        assertThat(AppBindingKeys.toFlatKey("crm")).isEqualTo("CRM");
    }

    @Test
    void testIsValidKey() {
        assertThat(AppBindingKeys.isValidKey("approve_refund")).isTrue();
        assertThat(AppBindingKeys.isValidKey("a-b")).isTrue();
        assertThat(AppBindingKeys.isValidKey("Approve")).isFalse();
        assertThat(AppBindingKeys.isValidKey("1approve")).isFalse();
        assertThat(AppBindingKeys.isValidKey(null)).isFalse();
        assertThat(AppBindingKeys.isValidKey("a".repeat(65))).isFalse();
    }
}
