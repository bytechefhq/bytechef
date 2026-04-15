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
 * @author Ivica Cardic
 */
class ResourceVisibilityTest {

    @Test
    void testOrdinalsMatchThePersistedValues() {
        assertThat(ResourceVisibility.values()).containsExactly(
            ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE, ResourceVisibility.ORGANIZATION);

        assertThat(ResourceVisibility.PRIVATE.ordinal()).isZero();
        assertThat(ResourceVisibility.WORKSPACE.ordinal()).isEqualTo(1);
        assertThat(ResourceVisibility.ORGANIZATION.ordinal()).isEqualTo(2);
    }

    @Test
    void testIsAtLeastFollowsTheReachOrder() {
        assertThat(ResourceVisibility.PRIVATE.isAtLeast(ResourceVisibility.WORKSPACE)).isFalse();
        assertThat(ResourceVisibility.WORKSPACE.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.ORGANIZATION.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
    }
}
