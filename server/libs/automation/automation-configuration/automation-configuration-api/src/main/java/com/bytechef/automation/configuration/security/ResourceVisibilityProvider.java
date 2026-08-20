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

package com.bytechef.automation.configuration.security;

import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import java.io.Serializable;
import java.util.Optional;

/**
 * SPI contributed once per resource family so {@code PermissionService} can read a single resource's visibility without
 * depending on that resource's module.
 *
 * <p>
 * Complements {@code ResourceOwnershipResolver}, which answers the workspace and owner questions; this one answers
 * reach. A resource type that contributes no provider is simply unrestricted by visibility — ownership and scope checks
 * still apply to it.
 *
 * <p>
 * Implementations MUST fail closed: an unknown id returns {@link Optional#empty()}, which callers treat as not visible
 * rather than as unrestricted.
 *
 * @author Ivica Cardic
 */
public interface ResourceVisibilityProvider {

    /**
     * Discriminator matching {@code ResourceOwnershipResolver.resourceType()} and
     * {@code ResourceVisibilityPolicy.resourceType()} — e.g. {@code "Connection"}, {@code "Project"},
     * {@code "Workflow"}. Must be unique across providers.
     */
    String resourceType();

    /**
     * The resource's visibility and creator, or empty when it does not exist.
     */
    Optional<VisibilityRecord> fetchVisibility(long id);

    /**
     * Id-shape-agnostic entry point used by {@code PermissionService}. The default handles numeric ids; providers for
     * string-keyed resources (workflows) override it. A non-numeric id on the default fails closed.
     */
    default Optional<VisibilityRecord> fetchVisibility(Serializable id) {
        if (id instanceof Number number) {
            return fetchVisibility(number.longValue());
        }

        return Optional.empty();
    }

    /**
     * The resource type under which the returned record's visibility and grants are stored. A resource that inherits
     * its reach returns its parent's type ({@code "Project"}) and its record's id is the parent's id, so grant lookups
     * resolve against the parent. Defaults to {@link #resourceType()}.
     */
    default String visibilityResourceType() {
        return resourceType();
    }
}
