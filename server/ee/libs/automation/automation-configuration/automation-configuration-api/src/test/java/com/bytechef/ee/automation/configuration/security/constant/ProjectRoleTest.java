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
 * Pins the {@link ProjectRole#hasAtLeast(ProjectRole)} contract. These cases used to be silently broken: passing
 * {@code null} would NPE on {@code required.ordinal()} with no caller-friendly message, and no test covered reflexive
 * or transitive behavior so a subtle inversion (e.g., {@code ordinal() >= required.ordinal()}) would slip past review.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectRoleTest {

    @Test
    void testHasAtLeastIsReflexive() {
        for (ProjectRole role : ProjectRole.values()) {
            assertThat(role.hasAtLeast(role))
                .as("%s should satisfy hasAtLeast(%s)", role, role)
                .isTrue();
        }
    }

    @Test
    void testHasAtLeastHonorsHierarchyOrder() {
        // ADMIN is the most privileged (ordinal 0). Every role satisfies hasAtLeast(VIEWER), only ADMIN satisfies
        // hasAtLeast(ADMIN).
        assertThat(ProjectRole.ADMIN.hasAtLeast(ProjectRole.VIEWER)).isTrue();
        assertThat(ProjectRole.ADMIN.hasAtLeast(ProjectRole.EDITOR)).isTrue();
        assertThat(ProjectRole.EDITOR.hasAtLeast(ProjectRole.VIEWER)).isTrue();
        assertThat(ProjectRole.OPERATOR.hasAtLeast(ProjectRole.VIEWER)).isTrue();

        assertThat(ProjectRole.VIEWER.hasAtLeast(ProjectRole.ADMIN)).isFalse();
        assertThat(ProjectRole.VIEWER.hasAtLeast(ProjectRole.EDITOR)).isFalse();
        assertThat(ProjectRole.EDITOR.hasAtLeast(ProjectRole.ADMIN)).isFalse();
        assertThat(ProjectRole.OPERATOR.hasAtLeast(ProjectRole.ADMIN)).isFalse();
    }

    @Test
    void testHasAtLeastIsTransitive() {
        // If ADMIN hasAtLeast EDITOR and EDITOR hasAtLeast VIEWER, then ADMIN hasAtLeast VIEWER.
        assertThat(ProjectRole.ADMIN.hasAtLeast(ProjectRole.EDITOR)).isTrue();
        assertThat(ProjectRole.EDITOR.hasAtLeast(ProjectRole.VIEWER)).isTrue();
        assertThat(ProjectRole.ADMIN.hasAtLeast(ProjectRole.VIEWER)).isTrue();
    }

    @Test
    void testHasAtLeastRejectsNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> ProjectRole.ADMIN.hasAtLeast(null))
            .withMessageContaining("required");
    }
}
