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
 * Defines a single cluster element, which is a reusable building block (such as a model, tool, or memory) that can be
 * plugged into a cluster-based component like an AI agent. A cluster element declares its metadata, input properties,
 * output schema, and the underlying element implementation it wraps.
 *
 * @param <T> the type of the underlying element implementation this definition wraps
 * @author Ivica Cardic
 */
public interface ClusterElementDefinition<T> {

    /**
     * Returns the human-readable description of the cluster element.
     *
     * @return an {@code Optional} containing the description if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * Returns the underlying element implementation this definition wraps.
     *
     * @return the wrapped element implementation
     */
    T getElement();

    /**
     * Returns the optional {@link Help} content that provides additional guidance to users configuring the cluster
     * element.
     *
     * @return an {@code Optional} containing the help content if defined, or an empty {@code Optional} otherwise
     */
    default Optional<Help> getHelp() {
        return Optional.empty();
    }

    /**
     * Returns the optional {@link OutputDefinition} that describes the schema and sample of the cluster element's
     * output.
     *
     * @return an {@code Optional} containing the output definition if defined, or an empty {@code Optional} otherwise
     */
    default Optional<OutputDefinition> getOutputDefinition() {
        return Optional.empty();
    }

    /**
     * Returns the unique name that identifies this cluster element within its component.
     *
     * @return the cluster element name
     */
    String getName();

    /**
     * Returns the optional function used to map an error response to a {@link ProviderException} during the cluster
     * element's execution.
     *
     * @return an {@code Optional} containing the error-processing function if defined, or an empty {@code Optional}
     *         otherwise
     */
    default Optional<ProcessErrorResponseFunction> getProcessErrorResponse() {
        return Optional.empty();
    }

    /**
     * Returns the input properties that define the parameters the cluster element accepts.
     *
     * @return the list of properties, or an empty list if none are defined
     */
    default List<? extends Property> getProperties() {
        return List.of();
    }

    /**
     * Returns the human-readable title of the cluster element displayed in the user interface.
     *
     * @return an {@code Optional} containing the title if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getTitle() {
        return Optional.empty();
    }

    /**
     * Returns the {@link ClusterElementType} that classifies this cluster element and specifies how it plugs into a
     * cluster-based component.
     *
     * @return the cluster element type
     */
    ClusterElementType getType();

    /**
     * Returns the optional function that dynamically produces the description shown for the cluster element's node in
     * the workflow editor.
     *
     * @return an {@code Optional} containing the workflow-node-description function if defined, or an empty
     *         {@code Optional} otherwise
     */
    default Optional<WorkflowNodeDescriptionFunction> getWorkflowNodeDescription() {
        return Optional.empty();
    }

    /**
     * Functional interface that dynamically supplies the list of selectable options for a cluster element's property,
     * optionally filtered by a search term and dependent on the values of other properties.
     *
     * @param <T> the type of the value held by each produced {@link Option}
     */
    @FunctionalInterface
    interface OptionsFunction<T> extends OptionsDataSource.BaseOptionsFunction {

        /**
         * Produces the list of options available for a property given the current input and connection parameters.
         *
         * @param inputParameters      the current input parameters of the cluster element
         * @param connectionParameters the parameters of the connection used by the cluster element
         * @param lookupDependsOnPaths the property paths whose values this lookup depends on, mapped to their resolved
         *                             values
         * @param searchText           the text used to filter the returned options, or an empty string for no filtering
         * @param context              the cluster element context providing access to runtime utilities and services
         * @return the list of available options
         * @throws Exception if an error occurs while producing the options
         */
        List<? extends Option<T>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            String searchText, ClusterElementContext context) throws Exception;
    }

    /**
     * Function that computes the output schema and sample output of a cluster element from its input and connection
     * parameters.
     */
    interface OutputFunction extends BaseOutputFunction {

        /**
         * Produces the output response describing the cluster element's schema and sample output.
         *
         * @param inputParameters      the current input parameters of the cluster element
         * @param connectionParameters the parameters of the connection used by the cluster element
         * @param context              the cluster element context providing access to runtime utilities and services
         * @return the {@link OutputResponse} describing the output schema and sample
         * @throws Exception if an error occurs while producing the output response
         */
        OutputResponse apply(Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context)
            throws Exception;
    }

    /**
     * Functional interface that maps a failed HTTP response produced during a cluster element's execution to a
     * {@link ProviderException}.
     */
    @FunctionalInterface
    interface ProcessErrorResponseFunction {

        /**
         * Maps a failed HTTP response to a {@link ProviderException}.
         *
         * @param statusCode the HTTP status code of the error response
         * @param body       the response body associated with the error
         * @param headers    the HTTP response headers, keyed by header name
         * @param context    the cluster element context for the current execution
         * @return a {@link ProviderException} representing the mapped remote error
         * @throws Exception if the error response cannot be mapped to a {@link ProviderException}
         */
        ProviderException
            apply(int statusCode, Object body, Map<String, List<String>> headers, ClusterElementContext context)
                throws Exception;
    }

    /**
     * Functional interface that dynamically supplies the list of input properties for a cluster element, allowing the
     * property set to depend on the current input and connection parameters.
     */
    @FunctionalInterface
    interface PropertiesFunction extends PropertiesDataSource.BasePropertiesFunction {

        /**
         * Produces the list of value properties available for the cluster element given the current input and
         * connection parameters.
         *
         * @param inputParameters      the current input parameters of the cluster element
         * @param connectionParameters the parameters of the connection used by the cluster element
         * @param lookupDependsOnPaths the property paths whose values this lookup depends on, mapped to their resolved
         *                             values
         * @param context              the cluster element context providing access to runtime utilities and services
         * @return the list of dynamically resolved value properties
         * @throws Exception if an error occurs while producing the properties
         */
        List<? extends Property.ValueProperty<?>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            ClusterElementContext context) throws Exception;
    }

    /**
     * Functional interface that dynamically produces the description text shown for a cluster element's node in the
     * workflow editor based on the current input parameters.
     */
    @FunctionalInterface
    interface WorkflowNodeDescriptionFunction {

        /**
         * Produces the description shown for the cluster element's workflow node.
         *
         * @param inputParameters the current input parameters of the cluster element
         * @param context         the cluster element context providing access to runtime utilities and services
         * @return the description text for the workflow node
         * @throws Exception if an error occurs while producing the description
         */
        String apply(Parameters inputParameters, ClusterElementContext context) throws Exception;
    }

    /**
     * Classifies a cluster element and specifies how it plugs into a cluster-based component, including whether the
     * slot accepts multiple elements and whether it is required.
     *
     * @param name             the name identifying the cluster element type
     * @param key              the key under which elements of this type are stored within the cluster
     * @param label            the human-readable label for the type displayed in the user interface
     * @param multipleElements whether the slot for this type accepts more than one element
     * @param required         whether at least one element of this type is required
     */
    record ClusterElementType(
        String name, String key, String label, boolean multipleElements, boolean required) {

        /**
         * Creates a cluster element type that accepts a single element.
         *
         * @param name     the name identifying the cluster element type
         * @param key      the key under which elements of this type are stored within the cluster
         * @param label    the human-readable label for the type displayed in the user interface
         * @param required whether an element of this type is required
         */
        public ClusterElementType(String name, String key, String label, boolean required) {
            this(name, key, label, false, required);
        }

        /**
         * Creates an optional cluster element type that accepts a single element.
         *
         * @param name  the name identifying the cluster element type
         * @param key   the key under which elements of this type are stored within the cluster
         * @param label the human-readable label for the type displayed in the user interface
         */
        public ClusterElementType(String name, String key, String label) {
            this(name, key, label, false, false);
        }
    }
}
