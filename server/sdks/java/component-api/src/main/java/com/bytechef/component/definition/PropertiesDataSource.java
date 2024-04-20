/*
 * Copyright 2023-present ByteChef Inc.
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

import com.bytechef.component.definition.Property.ValueProperty;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface PropertiesDataSource<F extends PropertiesDataSource.PropertiesFunction> {

    /**
     *
     * @return
     */
    List<String> getLoadPropertiesDependsOn();

    /**
     * The function that returns a list of properties.
     *
     * @return The function implementation
     */
    F getProperties();

    /**
     *
     */
    interface PropertiesFunction {
    }

    /**
     *
     */
    @FunctionalInterface
    interface ActionPropertiesFunction extends PropertiesFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param loadDependsOnPaths
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends ValueProperty<?>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> loadDependsOnPaths,
            ActionContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface TriggerPropertiesFunction extends PropertiesFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param loadDependsOnPaths
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends ValueProperty<?>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> loadDependsOnPaths,
            TriggerContext context) throws Exception;
    }
}
