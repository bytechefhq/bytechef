/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ResolvedRoleTest {

    @Test
    void testAcceptsABuiltInRole() {
        assertThat(new ResolvedRole(WorkspaceRole.EDITOR, null).workspaceRole()).isEqualTo(WorkspaceRole.EDITOR);
    }

    @Test
    void testAcceptsACustomRole() {
        assertThat(new ResolvedRole(null, 9L).customRoleId()).isEqualTo(9L);
    }

    @Test
    void testRejectsNeitherRole() {
        assertThatThrownBy(() -> new ResolvedRole(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRejectsBothRoles() {
        assertThatThrownBy(() -> new ResolvedRole(WorkspaceRole.ADMIN, 9L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
