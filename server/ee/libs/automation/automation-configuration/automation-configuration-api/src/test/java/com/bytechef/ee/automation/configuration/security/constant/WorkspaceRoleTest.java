/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link WorkspaceRole#hasAtLeast(WorkspaceRole)} contract. Mirror of {@link ProjectRoleTest}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceRoleTest {

    @Test
    void testHasAtLeastIsReflexive() {
        for (WorkspaceRole role : WorkspaceRole.values()) {
            assertThat(role.hasAtLeast(role))
                .as("%s should satisfy hasAtLeast(%s)", role, role)
                .isTrue();
        }
    }

    @Test
    void testHasAtLeastHonorsHierarchyOrder() {
        assertThat(WorkspaceRole.ADMIN.hasAtLeast(WorkspaceRole.VIEWER)).isTrue();
        assertThat(WorkspaceRole.ADMIN.hasAtLeast(WorkspaceRole.EDITOR)).isTrue();
        assertThat(WorkspaceRole.EDITOR.hasAtLeast(WorkspaceRole.VIEWER)).isTrue();

        assertThat(WorkspaceRole.VIEWER.hasAtLeast(WorkspaceRole.ADMIN)).isFalse();
        assertThat(WorkspaceRole.VIEWER.hasAtLeast(WorkspaceRole.EDITOR)).isFalse();
        assertThat(WorkspaceRole.EDITOR.hasAtLeast(WorkspaceRole.ADMIN)).isFalse();
    }

    @Test
    void testHasAtLeastIsTransitive() {
        assertThat(WorkspaceRole.ADMIN.hasAtLeast(WorkspaceRole.EDITOR)).isTrue();
        assertThat(WorkspaceRole.EDITOR.hasAtLeast(WorkspaceRole.VIEWER)).isTrue();
        assertThat(WorkspaceRole.ADMIN.hasAtLeast(WorkspaceRole.VIEWER)).isTrue();
    }

    @Test
    void testHasAtLeastRejectsNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> WorkspaceRole.ADMIN.hasAtLeast(null))
            .withMessageContaining("required");
    }
}
