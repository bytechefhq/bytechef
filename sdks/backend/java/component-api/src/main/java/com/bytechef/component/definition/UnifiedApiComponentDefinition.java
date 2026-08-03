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

package com.bytechef.component.definition;

import java.util.Optional;

/**
 * Extends a component definition with support for the unified API layer, which exposes a normalized model across
 * multiple providers within the same category (for example, CRM or ticketing).
 *
 * @author Ivica Cardic
 */
public interface UnifiedApiComponentDefinition {

    /**
     * Returns the unified API definition contributed by this component, when it participates in a unified API category.
     *
     * @return an {@link Optional} containing the unified API definition, or an empty {@link Optional} if the component
     *         does not provide one
     */
    default Optional<UnifiedApiDefinition> getUnifiedApi() {
        return Optional.empty();
    }
}
