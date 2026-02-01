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

package com.bytechef.platform.component.definition.datastream;

import com.bytechef.component.definition.ClusterElementContext.ClusterElementFunction;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;

/**
 * A functional interface designed to resolve cluster elements based on their type and a specified function. This
 * interface is part of the component system and provides an abstraction for processing and obtaining a specific cluster
 * element using custom logic defined via the provided function.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ClusterElementResolverFunction {

    /**
     * Resolves a cluster element based on its type and a provided processing function. The resolution logic is defined
     * by the supplied function, which applies custom operations on the cluster element.
     *
     * @param <T>                    the type of the result produced by the resolution process
     * @param clusterElementType     the type of cluster element to resolve
     * @param clusterElementFunction a function defining the logic to process and transform the resolved cluster element
     * @return the result obtained by applying the provided function to the resolved cluster element
     */
    <T> T resolve(ClusterElementType clusterElementType, ClusterElementFunction<T> clusterElementFunction);
}
