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

import java.util.Optional;

/**
 * An entry in a {@link WorkflowDefinition}'s task list: either a {@link TaskDefinition}, which performs work, or a
 * {@link CompositeTaskDefinition}, which groups other tasks so the engine can run them concurrently.
 *
 * @author Ivica Cardic
 */
public interface WorkflowTaskDefinition {

    /**
     * Returns the human-readable description of the entry, if one has been provided.
     *
     * @return an {@link Optional} containing the description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the display label of the entry, if one has been provided.
     *
     * @return an {@link Optional} containing the label, or an empty {@link Optional} if none is set
     */
    Optional<String> getLabel();

    /**
     * Returns the name that identifies this entry within its workflow. Names are flat: a task nested inside a composite
     * shares one namespace with every other task in the workflow, since that name is what the engine keys the task's
     * output by and what {@link TaskContext#input(String)} looks up.
     *
     * @return the name
     */
    String getName();
}
