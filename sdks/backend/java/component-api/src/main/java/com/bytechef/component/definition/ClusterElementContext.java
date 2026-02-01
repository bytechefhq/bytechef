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

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
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
     * Resolves a specific cluster element by applying the provided function to it. The cluster element is identified
     * using the specified {@code clusterElementType}.
     *
     * @param <T>                    the type of the result produced by the resolution process
     * @param clusterElementType     the type of the cluster element to resolve
     * @param clusterElementFunction the function to apply to the resolved cluster element for processing
     * @return the result of applying the function to the resolved cluster element
     */
    <T> T resolveClusterElement(
        ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction);

    /**
     * Represents a functional interface for applying operations to a specific cluster element in a cluster processing
     * context. The function is used to process or transform a provided cluster element using associated parameters and
     * the cluster context.
     *
     * @param <T> the type of the result produced by the application of this function
     */
    interface ClusterElementFunction<T> {

        /**
         * Applies the provided function to a specific cluster element for the purpose of processing or transforming it.
         * The function uses the provided input parameters, connection parameters, and the cluster element context for
         * execution.
         *
         * @param clusterElement       the cluster element to be processed or transformed
         * @param inputParameters      the parameters used to configure the operation for processing the cluster element
         * @param connectionParameters the parameters used to manage connections or interactions during processing
         * @param context              the context in which the cluster element is being processed
         * @return the result of applying the function to the provided cluster element
         */
        T apply(
            Object clusterElement, Parameters inputParameters, Parameters connectionParameters,
            ClusterElementContext context);
    }

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
         * Gets a value from a nested map using dot notation path.
         *
         * @param map  the source map
         * @param path the dot-separated path (e.g., "item.name")
         * @return the value at the path, or null if not found
         */
        Object getValue(Map<String, Object> map, String path);

        /**
         * Sets a value in a nested map using dot notation path. Creates intermediate maps as needed.
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
