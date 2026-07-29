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

package com.bytechef.component;

import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.Context;
import java.util.Map;
import java.util.Optional;

/**
 * Default component handler marker interface.
 *
 * @author Ivica Cardic
 */
public interface ComponentHandler {

    /**
     * This can be useful if we still want to have only one method to handle all actions instead of defining
     * <code>performFunction</code> for each <code>ActionDefinition</code>.
     *
     * @return optional HandleActionFunction
     */
    default Optional<ActionHandlerFunction> getActionHandler() {
        return Optional.empty();
    }

    /**
     * Returns the {@link ComponentDefinition} that describes this component, including its name, version, actions,
     * triggers, and connection configuration.
     *
     * @return the component definition backing this handler
     */
    ComponentDefinition getDefinition();

    /**
     * Returns the unique name of this component as declared in its {@link ComponentDefinition}.
     *
     * @return the component name
     */
    default String getName() {
        ComponentDefinition componentDefinition = getDefinition();

        return componentDefinition.getName();
    }

    /**
     * Returns the version of this component as declared in its {@link ComponentDefinition}.
     *
     * @return the component version
     */
    default int getVersion() {
        ComponentDefinition componentDefinition = getDefinition();

        return componentDefinition.getVersion();
    }

    /**
     * Functional interface for a single handler that dispatches the execution of all of a component's actions, keyed by
     * action name. It offers an alternative to defining a separate perform function for each individual
     * {@link com.bytechef.component.definition.ActionDefinition}.
     */
    @FunctionalInterface
    interface ActionHandlerFunction {

        /**
         * Executes the action identified by the given name using the supplied input parameters and context.
         *
         * @param actionName      the name of the action to execute
         * @param inputParameters the input parameters supplied to the action
         * @param context         the execution context providing access to runtime utilities and services
         * @return a map holding the result produced by the action
         * @throws Exception if an error occurs while executing the action
         */
        Map<String, ?> apply(String actionName, Map<String, ?> inputParameters, Context context) throws Exception;
    }
}
