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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aggregates every {@link ResourceVisibilityPolicy} on the classpath into one lookup, validating at construction that
 * resource types are unique and that each declared default is itself supported. Both violations are programming errors
 * that should stop the context from starting rather than surface as a puzzling rejection at the first write.
 *
 * <p>
 * An unregistered resource type fails closed: {@link #supports} returns {@code false} rather than defaulting to
 * permissive, so a resource that has not opted into visibility cannot have a rung written to it by accident.
 *
 * @author Ivica Cardic
 */
public final class ResourceVisibilityPolicyRegistry {

    private final Map<String, ResourceVisibilityPolicy> policies = new HashMap<>();

    public ResourceVisibilityPolicyRegistry(List<ResourceVisibilityPolicy> resourceVisibilityPolicies) {
        for (ResourceVisibilityPolicy resourceVisibilityPolicy : resourceVisibilityPolicies) {
            String resourceType = resourceVisibilityPolicy.resourceType();

            ResourceVisibilityPolicy previousPolicy = policies.put(resourceType, resourceVisibilityPolicy);

            if (previousPolicy != null) {
                throw new IllegalStateException(
                    "Duplicate ResourceVisibilityPolicy for resource type '" + resourceType + "'");
            }

            Set<ResourceVisibility> supportedVisibilities = resourceVisibilityPolicy.supportedVisibilities();

            if (!supportedVisibilities.contains(resourceVisibilityPolicy.defaultVisibility())) {
                throw new IllegalStateException(
                    "ResourceVisibilityPolicy for '" + resourceType + "' declares default " +
                        resourceVisibilityPolicy.defaultVisibility() + " which is not in its supported set " +
                        supportedVisibilities);
            }
        }
    }

    /**
     * Returns whether {@code visibility} is a legal rung for {@code resourceType}. An unregistered resource type
     * returns {@code false}.
     */
    public boolean supports(String resourceType, ResourceVisibility visibility) {
        ResourceVisibilityPolicy resourceVisibilityPolicy = policies.get(resourceType);

        if (resourceVisibilityPolicy == null) {
            return false;
        }

        Set<ResourceVisibility> supportedVisibilities = resourceVisibilityPolicy.supportedVisibilities();

        return supportedVisibilities.contains(visibility);
    }

    /**
     * Returns the creation default for {@code resourceType}.
     *
     * @throws IllegalArgumentException when the resource type has no registered policy
     */
    public ResourceVisibility defaultVisibility(String resourceType) {
        ResourceVisibilityPolicy resourceVisibilityPolicy = policies.get(resourceType);

        if (resourceVisibilityPolicy == null) {
            throw new IllegalArgumentException("No ResourceVisibilityPolicy registered for '" + resourceType + "'");
        }

        return resourceVisibilityPolicy.defaultVisibility();
    }
}
