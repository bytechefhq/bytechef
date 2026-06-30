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

import java.io.Serializable;
import java.util.OptionalLong;

/**
 * SPI contributed once per resource family to map a resource id to its owning coordinates. Consumed by
 * {@code PermissionService.hasResourceScope} / {@code isResourceOwner} via a registry keyed on {@link #resourceType()}.
 * Implementations MUST fail closed: an unknown / missing resource returns {@link ResourceOwner#unknown()} rather than
 * throwing.
 *
 * @author Ivica Cardic
 */
public interface ResourceOwnershipResolver {

    /**
     * Resource-type discriminator matching the {@code @PreAuthorize} token prefix, e.g. {@code "Connection"} for the
     * token {@code 'Connection'}. Must be unique across all registered resolvers.
     */
    String resourceType();

    /**
     * Owning coordinates for the given numeric resource id. Returns {@link ResourceOwner#unknown()} when the resource
     * does not exist or its owner cannot be determined.
     */
    ResourceOwner resolveOwner(long id);

    /**
     * Owning coordinates for a resource id of any {@link Serializable} key type. The default handles the common numeric
     * case by delegating to {@link #resolveOwner(long)}; String-keyed resources (e.g. a workflow UUID) override this.
     * Fails closed (returns {@link ResourceOwner#unknown()}) for unexpected id types.
     */
    default ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }

    /**
     * Owning coordinates of a resource. A resolver may populate {@code workspaceId} (workspace-mapped resources),
     * {@code ownerUserId} (user-owned resources), or both.
     */
    record ResourceOwner(OptionalLong workspaceId, OptionalLong ownerUserId) {

        public static ResourceOwner unknown() {
            return new ResourceOwner(OptionalLong.empty(), OptionalLong.empty());
        }

        public static ResourceOwner ofWorkspace(long workspaceId) {
            return new ResourceOwner(OptionalLong.of(workspaceId), OptionalLong.empty());
        }

        public static ResourceOwner ofUser(long ownerUserId) {
            return new ResourceOwner(OptionalLong.empty(), OptionalLong.of(ownerUserId));
        }

        public static ResourceOwner of(OptionalLong workspaceId, OptionalLong ownerUserId) {
            return new ResourceOwner(workspaceId, ownerUserId);
        }
    }
}
