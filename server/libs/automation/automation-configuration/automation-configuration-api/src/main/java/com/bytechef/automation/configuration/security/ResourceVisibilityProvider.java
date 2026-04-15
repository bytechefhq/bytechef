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
     * {@code ResourceVisibilityPolicy.resourceType()} — e.g. {@code "Connection"}. Must be unique across providers.
     */
    String resourceType();

    /**
     * The resource's visibility and creator, or empty when it does not exist.
     */
    Optional<VisibilityRecord> fetchVisibility(long id);
}
