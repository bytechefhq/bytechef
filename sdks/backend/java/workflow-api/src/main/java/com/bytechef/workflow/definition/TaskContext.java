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

import java.util.Map;

/**
 * Provides a {@link TaskDefinition.PerformFunction} with access to the surrounding execution engine at the moment the
 * task performs its work.
 *
 * <p>
 * An instance is supplied by the engine when it invokes {@link TaskDefinition.PerformFunction#apply(TaskContext)},
 * letting a task's perform logic invoke other components and emit log messages without depending on the engine's
 * concrete implementation.
 *
 * @author Ivica Cardic
 */
public interface TaskContext {

    /**
     * Invokes an action of another component and returns its result.
     *
     * @param componentName  the name of the component that defines the action
     * @param actionName     the name of the action to invoke
     * @param input          the input parameters passed to the action, keyed by parameter name
     * @param connectionName the name of the connection to use when invoking the action
     * @return the value produced by the invoked action, or {@code null} if it yields no result
     * @throws Exception if the action cannot be invoked or fails while executing
     */
    Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception;

    /**
     * Returns the parameters of one of the task's wired connections — the non-secret configuration a task may need to
     * build a request itself (a region, tenant or account id, a base URL), alongside the credentials the platform
     * otherwise applies for it when a component action is invoked with the connection.
     *
     * <p>
     * The name must be one the task declared and the user wired; nothing else is reachable. Note the returned map
     * carries the connection's credential values too, so treat it as sensitive.
     *
     * @param connectionName the name of the connection, as declared on the task
     * @return the connection's parameters, keyed by parameter name
     * @throws Exception if no connection with that name is wired to the task
     */
    Map<String, ?> connection(String connectionName) throws Exception;

    /**
     * Logs a message at the given severity level.
     *
     * @param level   the severity level of the message, e.g. {@code "info"}, {@code "warn"}, or {@code "error"}
     * @param message the message to log
     */
    void log(String level, String message);
}
