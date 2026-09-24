/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AuditEventGraphQlControllerTest {

    @Test
    void testMaskPrincipalReturnsNullForNull() {
        assertThat(AuditEventGraphQlController.maskPrincipal(null)).isNull();
    }

    @Test
    void testMaskPrincipalReturnsBlankUnchanged() {
        assertThat(AuditEventGraphQlController.maskPrincipal("")).isEmpty();
        assertThat(AuditEventGraphQlController.maskPrincipal("   ")).isEqualTo("   ");
    }

    @Test
    void testMaskPrincipalMasksEmail() {
        assertThat(AuditEventGraphQlController.maskPrincipal("alice@example.com")).isEqualTo("a***@example.com");
    }

    @Test
    void testMaskPrincipalMasksNonEmail() {
        assertThat(AuditEventGraphQlController.maskPrincipal("alice")).isEqualTo("a***");
    }
}
