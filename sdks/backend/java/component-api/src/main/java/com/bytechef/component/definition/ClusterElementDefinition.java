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

import com.bytechef.component.exception.ProviderException;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseOutputFunction;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface ClusterElementDefinition<T> {

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    /**
     *
     * @return
     */
    T getElement();

    /**
     *
     * @return
     */
    Optional<Help> getHelp();

    /**
     *
     * @return
     */
    Optional<OutputDefinition> getOutputDefinition();

    /**
     *
     * @return
     */
    String getName();

    Optional<ClusterElementDefinition.ProcessErrorResponseFunction> getProcessErrorResponse();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    ClusterElementType getType();

    /**
     *
     * @return
     */
    Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription();

    /**
     *
     */
    @FunctionalInterface
    interface OptionsFunction<T> extends OptionsDataSource.BaseOptionsFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param lookupDependsOnPaths
         * @param searchText
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends Option<T>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            String searchText, ClusterElementContext context) throws Exception;
    }

    /**
     *
     */
    interface OutputFunction extends BaseOutputFunction {

        /**
         * @param inputParameters
         * @param connectionParameters
         * @param context
         * @return
         */
        OutputResponse apply(Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context)
            throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ProcessErrorResponseFunction {

        /**
         *
         * @param statusCode
         * @param body
         * @param context
         * @return
         */
        ProviderException apply(int statusCode, Object body, ClusterElementContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PropertiesFunction extends PropertiesDataSource.BasePropertiesFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param lookupDependsOnPaths
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends Property.ValueProperty<?>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            ClusterElementContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface WorkflowNodeDescriptionFunction {

        /**
         * @param inputParameters
         * @param context
         * @return
         */
        String apply(Parameters inputParameters, ClusterElementContext context) throws Exception;
    }

    /**
     *
     */
    record ClusterElementType(
        String name, String key, String label, boolean multipleElements, boolean required) {

        public ClusterElementType(String name, String key, String label, boolean required) {
            this(name, key, label, false, required);
        }

        public ClusterElementType(String name, String key, String label) {
            this(name, key, label, false, false);
        }
    }
}
