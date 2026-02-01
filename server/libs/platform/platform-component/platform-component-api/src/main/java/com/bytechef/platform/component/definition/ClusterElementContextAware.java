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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Parameters;

/**
 * Extends the functionality of the {@link ClusterElementContext} and {@link JobContextAware} interfaces to provide
 * additional context-aware operations and functionality for working with cluster elements. <br>
 * This interface enables interaction with cluster elements in a processing context by allowing the resolution of
 * specific cluster elements and applying operations to them using defined functions.
 *
 * @author Ivica Cardic
 */
public interface ClusterElementContextAware extends ClusterElementContext, JobContextAware {

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
}
