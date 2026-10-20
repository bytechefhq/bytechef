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

import com.bytechef.definition.BaseOutputDefinition;
import com.bytechef.definition.BaseOutputFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used for specifying a task dispatcher description.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public interface TaskDispatcherDefinition {

    int VERSION_1 = 1;

    /**
     * Tells whether the task dispatcher is deprecated and should be replaced in workflows that still use it.
     *
     * @return {@code true} if the task dispatcher is deprecated, {@code false} otherwise
     */
    default boolean getDeprecated() {
        return false;
    }

    /**
     * Returns the human-readable description of the task dispatcher, explaining what it does.
     *
     * @return an {@code Optional} containing the description if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getDescription() {
        return Optional.empty();
    }

    /**
     * Returns the help content, such as a description and a learn-more link, shown alongside the task dispatcher.
     *
     * @return an {@code Optional} containing the help if defined, or an empty {@code Optional} otherwise
     */
    default Optional<Help> getHelp() {
        return Optional.empty();
    }

    /**
     * Returns the icon representing the task dispatcher in the user interface, typically as SVG markup or a reference
     * to an icon resource.
     *
     * @return an {@code Optional} containing the icon if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getIcon() {
        return Optional.empty();
    }

    /**
     * Returns the unique name that identifies this task dispatcher.
     *
     * @return the task dispatcher name
     */
    String getName();

    /**
     * Returns the output definition, which describes the output schema or the function that computes it.
     *
     * @return an {@code Optional} containing the output definition if defined, or an empty {@code Optional} otherwise
     */
    default Optional<OutputDefinition> getOutputDefinition() {
        return Optional.empty();
    }

    /**
     * Returns the input properties of the task dispatcher.
     *
     * @return the list of input properties, or an empty list if none are defined
     */
    default List<? extends Property> getProperties() {
        return List.of();
    }

    /**
     * Returns the resources, such as the documentation URL, associated with the task dispatcher.
     *
     * @return an {@code Optional} containing the resources if defined, or an empty {@code Optional} otherwise
     */
    default Optional<Resources> getResources() {
        return Optional.empty();
    }

    /**
     * Returns the properties that hold the tasks the dispatcher runs, such as the iteratee of a loop or the branches of
     * a condition.
     *
     * @return the list of task properties, or an empty list if none are defined
     */
    default List<? extends Property> getTaskProperties() {
        return List.of();
    }

    /**
     * Returns the human-readable title of the task dispatcher displayed in the user interface.
     *
     * @return an {@code Optional} containing the title if defined, or an empty {@code Optional} otherwise
     */
    default Optional<String> getTitle() {
        return Optional.empty();
    }

    /**
     * Returns the function that computes the variables the task dispatcher exposes to its child tasks.
     *
     * @return an {@code Optional} containing the variable properties function if defined, or an empty {@code Optional}
     *         otherwise
     */
    default Optional<VariablePropertiesFunction> getVariableProperties() {
        return Optional.empty();
    }

    /**
     * Returns the version of the task dispatcher, used to distinguish successive revisions of its definition.
     *
     * @return the task dispatcher version
     */
    default int getVersion() {
        return VERSION_1;
    }

    /**
     *
     */
    @FunctionalInterface
    interface OutputFunction extends BaseOutputFunction {

        /**
         * @param inputParameters
         * @return
         */
        BaseOutputDefinition.OutputResponse apply(Map<String, ?> inputParameters) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface PropertiesFunction {

        /**
         * @param inputParameters
         * @return
         */
        List<? extends Property> apply(Map<String, ?> inputParameters) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface OptionsFunction {

        /**
         * @param search
         * @return
         */
        List<? extends Option<String>> apply(String search) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface VariablePropertiesFunction {

        /**
         * @param inputParameters
         * @return
         */
        BaseOutputDefinition.OutputResponse apply(Map<String, ?> inputParameters) throws Exception;
    }
}
