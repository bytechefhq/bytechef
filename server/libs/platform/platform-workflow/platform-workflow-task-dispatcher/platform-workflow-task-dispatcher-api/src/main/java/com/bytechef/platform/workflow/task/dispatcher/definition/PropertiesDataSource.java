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

package com.bytechef.platform.workflow.task.dispatcher.definition;

import java.util.List;

/**
 * Resolves the nested properties of a {@link Property.DynamicPropertiesProperty} at runtime.
 *
 * @author Ivica Cardic
 */
public interface PropertiesDataSource {

    /**
     * Returns the names of the properties whose values the properties function needs, so the lookup is repeated when
     * any of them changes.
     *
     * @return the names of the dependent properties, or an empty list if the lookup depends on none
     */
    List<String> getPropertiesLookupDependsOn();

    /**
     * Returns the function that computes the nested properties from the current input parameters.
     *
     * @return the properties function
     */
    TaskDispatcherDefinition.PropertiesFunction getPropertiesFunction();
}
