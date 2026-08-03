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
 * Represents a named grouping of related value properties, allowing inputs to be organized into logical sections when
 * presented in the workflow editor. A single standalone property is modeled as a group containing exactly one property.
 *
 * @author Ivica Cardic
 */
public interface PropertyGroup {

    /**
     * Returns the technical name that uniquely identifies this property group.
     *
     * @return the group name
     */
    String getName();

    /**
     * Returns the human-readable label displayed for this group, when one is defined.
     *
     * @return an {@link Optional} containing the display label, or an empty {@link Optional} if none is set
     */
    default Optional<String> getLabel() {
        return Optional.empty();
    }

    /**
     * Returns the value properties that belong to this group.
     *
     * @return the list of properties contained in the group
     */
    List<? extends Property.ValueProperty<?>> getProperties();
}
