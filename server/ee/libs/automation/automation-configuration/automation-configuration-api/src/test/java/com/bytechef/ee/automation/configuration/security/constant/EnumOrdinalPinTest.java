/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EnumOrdinalPinTest {

    @Test
    void testWorkspaceRoleOrdinalsPinned() {
        assertThat(WorkspaceRole.ADMIN.ordinal()).isEqualTo(0);
        assertThat(WorkspaceRole.EDITOR.ordinal()).isEqualTo(1);
        assertThat(WorkspaceRole.VIEWER.ordinal()).isEqualTo(2);
        assertThat(WorkspaceRole.values()).hasSize(3);
    }

    @Test
    void testWorkspaceRolePrivilegeRanksPinned() {
        assertThat(WorkspaceRole.ADMIN.getPrivilegeRank()).isEqualTo(0);
        assertThat(WorkspaceRole.EDITOR.getPrivilegeRank()).isEqualTo(1);
        assertThat(WorkspaceRole.VIEWER.getPrivilegeRank()).isEqualTo(2);
    }
}
