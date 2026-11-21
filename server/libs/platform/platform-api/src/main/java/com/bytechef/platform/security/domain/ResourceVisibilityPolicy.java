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

import java.util.Set;

/**
 * SPI contributed once per resource family to declare which {@link ResourceVisibility} rungs the resource supports and
 * which one it is created with. Mirrors the {@code ResourceOwnershipResolver} / {@code PermissionScopeProvider}
 * per-module SPI shape, and is aggregated by {@link ResourceVisibilityPolicyRegistry}.
 *
 * <p>
 * Declaring the supported set per resource is what keeps the shared enum honest: {@code ORGANIZATION} is meaningful for
 * a credential that serves several workspaces, and meaningless for a resource that belongs to exactly one.
 *
 * @author Ivica Cardic
 */
public interface ResourceVisibilityPolicy {

    /**
     * Resource-type discriminator, matching {@code ResourceOwnershipResolver.resourceType()} — e.g. {@code
     * "Connection"}. Must be unique across all registered policies.
     */
    String resourceType();

    /**
     * The rung a resource of this type is created with. Must be a member of {@link #supportedVisibilities()}.
     */
    ResourceVisibility defaultVisibility();

    /**
     * Every rung this resource type may legally hold. A value outside this set is rejected server-side rather than by
     * convention.
     */
    Set<ResourceVisibility> supportedVisibilities();
}
