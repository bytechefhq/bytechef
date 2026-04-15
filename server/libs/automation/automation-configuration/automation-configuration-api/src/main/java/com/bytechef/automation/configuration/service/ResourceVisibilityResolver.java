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

package com.bytechef.automation.configuration.service;

import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.Collection;
import java.util.Set;

/**
 * Narrows an already workspace-scoped candidate set to the resources the current principal may see. CE resolves admin
 * bypass, reach and ownership; EE additionally consults resource grants.
 *
 * <p>
 * Returns ids rather than filtered objects so a single implementation serves every resource type without generics, and
 * so the in-memory implementation can later be swapped for a SQL predicate without moving callers.
 *
 * <p>
 * This is a filter, never a widener: callers pass a set already bounded by workspace, and the result is always a subset
 * of what they passed in.
 *
 * @author Ivica Cardic
 */
public interface ResourceVisibilityResolver {

    /**
     * @param resourceType the discriminator shared with {@code ResourceVisibilityPolicy} and
     *                     {@code ResourceOwnershipResolver}, e.g. {@code "Connection"}
     * @param workspaceId  the workspace whose resources are being listed
     * @param candidates   the workspace-scoped candidate set; never {@code null}
     * @return the ids of the candidates visible to the current principal. Never {@code null}; may be empty.
     */
    Set<Long> filterVisibleIds(String resourceType, long workspaceId, Collection<VisibilityRecord> candidates);

    /**
     * The three facts visibility resolution needs from any resource, so the resolver depends on no DTO type.
     *
     * @param id         the resource id
     * @param visibility the resource's reach
     * @param createdBy  the owner's login, as stored in the resource's {@code createdBy} audit column
     */
    record VisibilityRecord(long id, ResourceVisibility visibility, String createdBy) {
    }
}
