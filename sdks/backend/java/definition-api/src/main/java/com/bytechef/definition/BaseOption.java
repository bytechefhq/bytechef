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

package com.bytechef.definition;

import java.util.Optional;

/**
 * Represents a single selectable option offered by a property, pairing a human-readable label with the underlying value
 * that is stored when the option is chosen.
 *
 * @param <T> the type of the value held by the option
 * @author Ivica Cardic
 */
public interface BaseOption<T> {

    /**
     * Returns the optional description providing additional details about this option.
     *
     * @return an {@link Optional} containing the description, or an empty {@link Optional} if none is set
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * Returns the human-readable label displayed for this option.
     *
     * @return the option label
     */
    String getLabel();

    /**
     * Returns the underlying value associated with this option.
     *
     * @return the option value
     */
    T getValue();
}
