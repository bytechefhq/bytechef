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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public interface PropertiesDataSource {

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
    PropertiesFunction getProperties();

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
         * @param context
         * @return
         * @throws ComponentExecutionException
         */
        PropertiesResponse apply(
            ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context)
            throws ComponentExecutionException;
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
         * @param context
         * @return
         * @throws ComponentExecutionException
         */
        PropertiesResponse apply(
            ParameterMap inputParameters, ParameterMap connectionParameters, TriggerContext context)
            throws ComponentExecutionException;
    }

    /**
     *
     * @param properties
     * @param errorMessage
     */
    @SuppressFBWarnings("EI")
    record PropertiesResponse(List<? extends Property.ValueProperty<?>> properties, String errorMessage) {

        public PropertiesResponse(List<? extends Property.ValueProperty<?>> properties) {
            this(properties, null);
        }
    }
}
