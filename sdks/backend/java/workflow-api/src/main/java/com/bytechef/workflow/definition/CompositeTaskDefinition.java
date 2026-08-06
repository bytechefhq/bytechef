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

/**
 * Groups tasks so the engine runs them concurrently. A composite performs no work of its own — it names the group and
 * says how its members relate.
 *
 * <p>
 * Members of a {@link Type#PARALLEL} group are dispatched together, so none of them can read another's output: a
 * sibling has not completed when the group starts, and {@link TaskContext#input(String)} fails for a name that is not
 * in the snapshot. Whatever a group's members need must be produced before the group. Tasks within one
 * {@link Type#FORK_JOIN} branch do run in sequence and can read each other; the branches themselves cannot.
 *
 * @author Ivica Cardic
 */
public interface CompositeTaskDefinition extends WorkflowTaskDefinition {

    /**
     * Returns the branches of a {@link Type#FORK_JOIN} group — each a sequence of tasks, the sequences running
     * concurrently. Empty for a {@link Type#PARALLEL} group.
     *
     * @return the branches, each a list of tasks
     */
    List<? extends List<? extends TaskDefinition>> getBranches();

    /**
     * Returns the tasks of a {@link Type#PARALLEL} group, all dispatched at once. Empty for a {@link Type#FORK_JOIN}
     * group.
     *
     * @return the tasks
     */
    List<? extends TaskDefinition> getTasks();

    /**
     * Returns how this group's members relate.
     *
     * @return the group type
     */
    Type getType();

    /**
     * The kinds of grouping the engine can run.
     */
    enum Type {

        /**
         * Every member is dispatched at once. Members cannot read each other's output.
         */
        PARALLEL,

        /**
         * Each branch runs as its own sequence, the branches running concurrently.
         */
        FORK_JOIN
    }
}
