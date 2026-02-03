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

package com.bytechef.platform.component.script;

import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Interface for components to provide script input values for the code editor dialog. Implementations can generate
 * sample input data based on cluster element configuration.
 *
 * @author Ivica Cardic
 */
public interface CodeEditorScriptInputProvider {

    /**
     * Returns the root component name this provider handles.
     *
     * @return the root component name (e.g., "dataStream")
     */
    String getRootComponentName();

    /**
     * Returns sample script input values for a cluster element script editor. The provider should generate sample
     * values based on the data that would be available to the script at runtime.
     *
     * @param rootComponentVersion      the root component version
     * @param sourceComponentName       the component name of the SOURCE cluster element
     * @param sourceComponentVersion    the component version of the SOURCE cluster element
     * @param sourceClusterElementName  the cluster element name of the SOURCE
     * @param sourceInputParameters     the input parameters configured for the SOURCE cluster element
     * @param sourceComponentConnection the connection for the SOURCE component, if available
     * @return a map of input field names to sample values
     */
    Map<String, Object> getScriptInput(
        int rootComponentVersion, String sourceComponentName, int sourceComponentVersion,
        String sourceClusterElementName, Map<String, ?> sourceInputParameters,
        @Nullable ComponentConnection sourceComponentConnection);
}
