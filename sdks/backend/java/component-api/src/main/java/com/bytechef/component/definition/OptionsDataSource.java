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
 * Represents a data source that provides option retrieval functionalities. The type parameter {@code F} is a functional
 * interface extending {@link OptionsDataSource.BaseOptionsFunction}, used to define the logic for fetching or
 * processing options.
 *
 * @param <F> the type of the options function associated with this data source
 *
 * @author Ivica Cardic
 */
public interface OptionsDataSource<F extends OptionsDataSource.BaseOptionsFunction> {

    /**
     * Retrieves the options function associated with this data source.
     *
     * @return an instance of the options function of type {@code F}
     */
    F getOptions();

    /**
     * Retrieves an optional list of field names that this data source depends on for lookup operations.
     *
     * @return an {@code Optional} containing a list of field names if dependencies exist; otherwise, an empty
     *         {@code Optional}.
     */
    default Optional<List<String>> getOptionsLookupDependsOn() {
        return Optional.empty();
    }

    /**
     * Marker interface representing a base options function in the data source framework. This interface is intended to
     * be extended by more specific functional interfaces used for option lookup or retrieval operations, defining
     * custom behaviors depending on implementation requirements.
     */
    interface BaseOptionsFunction {
    }
}
