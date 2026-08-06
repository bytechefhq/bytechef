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

package com.bytechef.workflow.definition;

import java.util.List;
import java.util.Optional;

/**
 * Defines the structure of a single workflow: its identity, the inputs it accepts, the triggers that start it, the
 * tasks it performs, and the outputs it produces.
 *
 * <p>
 * A workflow definition is the declarative, executable unit that ByteChef schedules and runs. It is typically assembled
 * programmatically through the SDK's fluent DSL and consumed by the platform when registering and executing
 * automations.
 *
 * @author Ivica Cardic
 */
public interface WorkflowDefinition {

    /**
     * Returns the human-readable description of the workflow, if one has been provided.
     *
     * @return an {@link Optional} containing the workflow description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the inputs the workflow accepts when executed, if any have been declared.
     *
     * @return an {@link Optional} containing the list of workflow inputs, or an empty {@link Optional} if none are
     *         declared
     */
    Optional<List<? extends Input>> getInputs();

    /**
     * Returns the display label of the workflow, if one has been provided.
     *
     * @return an {@link Optional} containing the workflow label, or an empty {@link Optional} if none is set
     */
    Optional<String> getLabel();

    /**
     * Returns the unique name that identifies the workflow within its enclosing project or integration.
     *
     * @return the workflow name
     */
    String getName();

    /**
     * Returns the outputs the workflow produces once it completes, if any have been declared.
     *
     * @return an {@link Optional} containing the list of workflow outputs, or an empty {@link Optional} if none are
     *         declared
     */
    Optional<List<? extends Output>> getOutputs();

    /**
     * Returns the entries that make up the workflow's body, if any have been declared — {@link TaskDefinition}s that
     * perform work and {@link CompositeTaskDefinition}s that group tasks for concurrent execution.
     *
     * @return an {@link Optional} containing the list of entries, or an empty {@link Optional} if none are declared
     */
    Optional<List<? extends WorkflowTaskDefinition>> getTasks();

    /**
     * Returns the triggers that can initiate execution of the workflow, if any have been declared.
     *
     * @return an {@link Optional} containing the list of trigger definitions, or an empty {@link Optional} if none are
     *         declared
     */
    Optional<List<? extends TriggerDefinition>> getTriggers();
}
