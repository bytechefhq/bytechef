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

import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.util.Optional;

/**
 * Reports which environment a resource lives in, so that a by-id permission check can be answered against the role the
 * caller holds <em>there</em> rather than their role in the workspace as a whole.
 *
 * <p>
 * Contributing one is opt-in per resource type: a type with no resolver keeps the environment-unaware check it has
 * today. Only resource types that genuinely carry an environment should contribute — a project or a workflow definition
 * does not live in an environment, and inventing one for them would deny operations that are legitimately
 * environment-independent.
 *
 * <p>
 * This is deliberately separate from {@link ResourceOwnershipResolver} rather than a field on its
 * {@code ResourceOwner}: ownership answers "whose workspace is this", every resource type has an answer, and every
 * existing implementation would have had to change to say "no environment".
 *
 * @author Ivica Cardic
 */
public interface ResourceEnvironmentResolver {

    /**
     * The resource type key this resolver answers for, matching the type named in the {@code @PreAuthorize} expression.
     */
    String resourceType();

    /**
     * Returns the environment the resource lives in, or empty when it cannot be determined — a deleted row, or an id of
     * an unexpected shape. Empty falls back to the environment-unaware check rather than denying, so a resolver that
     * cannot answer never turns a working permission into a failure.
     */
    Optional<Environment> fetchEnvironment(Serializable id);
}
