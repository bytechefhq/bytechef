/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.security.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ResourceVisibility}.
 *
 * @author Ivica Cardic
 */
class ResourceVisibilityTest {

    /**
     * {@link ResourceVisibility} is persisted as an INT ordinal, and TWO liquibase changesets hard-code
     * {@code WORKSPACE}'s ordinal {@code 1} as the column default:
     *
     * <ul>
     * <li>{@code platform/connection/20260810000001_platform_connection_visibility.xml} — {@code connection.visibility}
     * <li>{@code automation/configuration/20260817000001_automation_configuration_project_visibility.xml} —
     * {@code project.visibility}
     * </ul>
     *
     * <p>
     * Reordering the constants would silently reinterpret every existing connection AND project row on upgrade — a
     * private resource read back as workspace-shared, with no error anywhere. This pins each ordinal explicitly so an
     * accidental reorder fails loud at build time; new values MUST be appended at the end of the declaration.
     */
    @Test
    void testResourceVisibilityOrdinalsAreStableForJdbcPersistence() {
        assertThat(ResourceVisibility.PRIVATE.ordinal()).isEqualTo(0);
        assertThat(ResourceVisibility.WORKSPACE.ordinal()).isEqualTo(1);
        assertThat(ResourceVisibility.ORGANIZATION.ordinal()).isEqualTo(2);
        assertThat(ResourceVisibility.values()).hasSize(3);
    }

    @Test
    void testIsAtLeastIsOrderedPrivateBelowWorkspaceBelowOrganization() {
        assertThat(ResourceVisibility.ORGANIZATION.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.WORKSPACE.isAtLeast(ResourceVisibility.PRIVATE)).isTrue();
        assertThat(ResourceVisibility.WORKSPACE.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.PRIVATE.isAtLeast(ResourceVisibility.WORKSPACE)).isFalse();
        assertThat(ResourceVisibility.WORKSPACE.isAtLeast(ResourceVisibility.ORGANIZATION)).isFalse();
    }
}
