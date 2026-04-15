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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ResourceVisibilityPolicyRegistry}.
 */
class ResourceVisibilityPolicyRegistryTest {

    private static ResourceVisibilityPolicy policy(
        String resourceType, ResourceVisibility defaultVisibility, Set<ResourceVisibility> supported) {

        return new ResourceVisibilityPolicy() {

            @Override
            public String resourceType() {
                return resourceType;
            }

            @Override
            public ResourceVisibility defaultVisibility() {
                return defaultVisibility;
            }

            @Override
            public Set<ResourceVisibility> supportedVisibilities() {
                // A fresh set per call, so SpotBugs sees no stored reference escaping. Set.copyOf would not do:
                // it short-circuits to the same instance when handed an already-immutable set.
                return new LinkedHashSet<>(supported);
            }
        };
    }

    @Test
    void testSupportsReturnsTrueForDeclaredRung() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(
            List.of(
                policy(
                    "Connection", ResourceVisibility.WORKSPACE,
                    Set.of(
                        ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE, ResourceVisibility.ORGANIZATION))));

        assertThat(registry.supports("Connection", ResourceVisibility.ORGANIZATION)).isTrue();
    }

    @Test
    void testSupportsReturnsFalseForUndeclaredRung() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(
            List.of(
                policy(
                    "Project", ResourceVisibility.WORKSPACE,
                    Set.of(ResourceVisibility.PRIVATE, ResourceVisibility.WORKSPACE))));

        assertThat(registry.supports("Project", ResourceVisibility.ORGANIZATION)).isFalse();
    }

    @Test
    void testSupportsFailsClosedForUnregisteredResourceType() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(List.of());

        assertThat(registry.supports("Unknown", ResourceVisibility.WORKSPACE)).isFalse();
    }

    @Test
    void testDefaultVisibilityIsReturnedPerResourceType() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(
            List.of(policy("Connection", ResourceVisibility.WORKSPACE, Set.of(ResourceVisibility.WORKSPACE))));

        assertThat(registry.defaultVisibility("Connection")).isEqualTo(ResourceVisibility.WORKSPACE);
    }

    @Test
    void testDefaultVisibilityThrowsForUnregisteredResourceType() {
        ResourceVisibilityPolicyRegistry registry = new ResourceVisibilityPolicyRegistry(List.of());

        assertThatThrownBy(() -> registry.defaultVisibility("Unknown"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown");
    }

    @Test
    void testDuplicateResourceTypeIsRegistrationError() {
        List<ResourceVisibilityPolicy> policies = List.of(
            policy("Connection", ResourceVisibility.WORKSPACE, Set.of(ResourceVisibility.WORKSPACE)),
            policy("Connection", ResourceVisibility.PRIVATE, Set.of(ResourceVisibility.PRIVATE)));

        assertThatThrownBy(() -> new ResourceVisibilityPolicyRegistry(policies))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Connection");
    }

    @Test
    void testDefaultMustBeInSupportedSet() {
        List<ResourceVisibilityPolicy> policies = List.of(
            policy("Connection", ResourceVisibility.ORGANIZATION, Set.of(ResourceVisibility.PRIVATE)));

        assertThatThrownBy(() -> new ResourceVisibilityPolicyRegistry(policies))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("default");
    }

    @Test
    void testIsAtLeastOrdersTheRungs() {
        assertThat(ResourceVisibility.ORGANIZATION.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.WORKSPACE.isAtLeast(ResourceVisibility.WORKSPACE)).isTrue();
        assertThat(ResourceVisibility.PRIVATE.isAtLeast(ResourceVisibility.WORKSPACE)).isFalse();
    }
}
