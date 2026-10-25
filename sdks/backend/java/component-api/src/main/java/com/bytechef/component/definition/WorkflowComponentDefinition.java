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

import java.util.List;
import java.util.Optional;

/**
 * Exposes the workflow-facing surface of a component: the actions and triggers it makes available within a workflow,
 * its component-level inputs, and its optional custom-action support.
 *
 * @author Ivica Cardic
 */
public interface WorkflowComponentDefinition {

    /**
     * Returns the actions this component contributes to workflows.
     *
     * @return the list of actions, or an empty list if none are defined
     */
    default List<ActionDefinition> getActions() {
        return List.of();
    }

    /**
     * Indicates whether this component supports user-defined custom actions.
     *
     * @return {@code true} when custom actions are supported, {@code false} otherwise
     */
    default boolean getCustomAction() {
        return false;
    }

    /**
     * Returns the contextual help shown for this component's custom action.
     *
     * @return an {@link Optional} containing the custom-action help, or an empty {@link Optional} if none is set
     */
    default Optional<Help> getCustomActionHelp() {
        return Optional.empty();
    }

    /**
     * Returns the component-level workflow inputs, always modeled as property groups (a lone property is a group with
     * one property).
     *
     * @return the list of input property groups, or an empty list if none are defined
     */
    default List<? extends PropertyGroup> getInputs() {
        return List.of();
    }

    /**
     * Returns the triggers this component contributes to workflows.
     *
     * @return the list of triggers, or an empty list if none are defined
     */
    default List<TriggerDefinition> getTriggers() {
        return List.of();
    }
}
