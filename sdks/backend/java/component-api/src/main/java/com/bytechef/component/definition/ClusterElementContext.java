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

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementContext extends Context {

    /**
     * Provides access to nested map utilities for working with dot notation paths.
     *
     * @param nestedFunction the function to apply to the Nested utilities
     * @param <R>            the return type
     * @return the result of applying the function
     */
    <R> R nested(ContextFunction<Nested, R> nestedFunction);

    /**
     * Provides utilities for working with nested map structures using dot notation paths.
     */
    interface Nested {

        /**
         * Checks if a nested path exists in the map.
         *
         * @param map  the source map
         * @param path the dot-separated path (e.g., "item.name")
         * @return true if the path exists (even if value is null), false otherwise
         */
        boolean containsPath(Map<String, Object> map, String path);

        /**
         * Flattens a nested map into a flat map with dot-notation keys.
         *
         * @param map the nested map to flatten
         * @return a flat map with dot-notation keys
         */
        Map<String, Object> flatten(Map<String, Object> map);

        /**
         * Gets a value from a nested map using the dot notation path.
         *
         * @param map  the source map
         * @param path the dot-separated path (e.g., "item.name")
         * @return the value at the path, or null if not found
         */
        Object getValue(Map<String, Object> map, String path);

        /**
         * Sets a value in a nested map using the dot notation path. Creates intermediate maps as needed.
         *
         * @param map   the target map
         * @param path  the dot-separated path (e.g., "item.name")
         * @param value the value to set
         */
        void setValue(Map<String, Object> map, String path, @Nullable Object value);

        /**
         * Unflattens a map with dot-notation keys into a nested map structure.
         *
         * @param map the flat map with dot-notation keys
         * @return a nested map structure
         */
        Map<String, Object> unflatten(Map<String, Object> map);
    }
}
