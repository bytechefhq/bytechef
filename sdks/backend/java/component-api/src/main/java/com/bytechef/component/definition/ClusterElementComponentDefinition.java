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

import java.util.List;
import java.util.Optional;

/**
 * Contract for a component that contributes cluster elements, which are the reusable building blocks (such as models,
 * tools, or memory) that can be assembled into cluster-based components like AI agents.
 *
 * @author Ivica Cardic
 */
public interface ClusterElementComponentDefinition {

    /**
     * Returns the cluster elements contributed by this component.
     *
     * @return an {@code Optional} containing the list of cluster element definitions if any are defined, or an empty
     *         {@code Optional} otherwise
     */
    Optional<List<ClusterElementDefinition<?>>> getClusterElements();
}
