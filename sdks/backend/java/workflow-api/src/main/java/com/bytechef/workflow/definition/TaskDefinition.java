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
 * Defines a single task within a {@link WorkflowDefinition}.
 *
 * <p>
 * A task represents one unit of work executed as part of a workflow. It is identified by its {@link #getName() name},
 * may carry {@link Parameter parameters} that configure it, and exposes a {@link PerformFunction} that encapsulates the
 * logic run when the task executes.
 *
 * @author Ivica Cardic
 */
public interface TaskDefinition {

    /**
     * Returns the human-readable description of the task, if one has been provided.
     *
     * @return an {@link Optional} containing the task description, or an empty {@link Optional} if none is set
     */
    Optional<String> getDescription();

    /**
     * Returns the display label of the task, if one has been provided.
     *
     * @return an {@link Optional} containing the task label, or an empty {@link Optional} if none is set
     */
    Optional<String> getLabel();

    /**
     * Returns the name that uniquely identifies this task within its workflow.
     *
     * @return the task name
     */
    String getName();

    /**
     * Returns the connections this task's perform uses when invoking component actions, if any have been declared. The
     * default implementation returns an empty {@link Optional}, so implementations that predate connection declarations
     * keep working unchanged.
     *
     * @return an {@link Optional} containing the declared connection requirements, or an empty {@link Optional} if none
     *         are declared
     */
    default Optional<List<? extends ConnectionRequirement>> getConnections() {
        return Optional.empty();
    }

    /**
     * Returns the parameters that configure the behavior of this task, if any have been declared.
     *
     * @return an {@link Optional} containing the list of configuration parameters, or an empty {@link Optional} if none
     *         are declared
     */
    Optional<List<? extends Parameter>> getParameters();

    /**
     * Returns the function that performs the task's work when the task is executed.
     *
     * @return the {@link PerformFunction} that carries out the task
     */
    PerformFunction getPerform();

    /**
     * Encapsulates the logic executed when a {@link TaskDefinition} runs.
     */
    interface PerformFunction {

        /**
         * Executes the task's logic and returns its result.
         *
         * @return the value produced by executing the task, or {@code null} if the task yields no result
         */
        Object apply();

        /**
         * Executes the task's logic with access to the engine's {@link TaskContext} and returns its result.
         *
         * <p>
         * Engines invoke this method rather than {@link #apply()}. The default implementation delegates to
         * {@link #apply()}, so implementations that only override the zero-argument method keep working unchanged.
         *
         * @param context the context through which the task can reach the surrounding execution engine
         * @return the value produced by executing the task, or {@code null} if the task yields no result
         * @throws Exception if the task fails while executing
         */
        default Object apply(TaskContext context) throws Exception {
            return apply();
        }
    }

    /**
     * Encapsulates the logic executed when a {@link TaskDefinition} runs, for implementations that need access to the
     * engine's {@link TaskContext}.
     *
     * <p>
     * This is a separate functional interface (rather than a lambda target for {@link PerformFunction}) so that
     * context-consuming task logic can still be supplied as a lambda, disambiguated from a zero-argument
     * {@link PerformFunction} lambda by arity.
     */
    interface ContextPerformFunction {

        /**
         * Executes the task's logic with access to the engine's {@link TaskContext} and returns its result.
         *
         * @param context the context through which the task can reach the surrounding execution engine
         * @return the value produced by executing the task, or {@code null} if the task yields no result
         * @throws Exception if the task fails while executing
         */
        Object apply(TaskContext context) throws Exception;
    }
}
