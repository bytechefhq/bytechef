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

import java.util.Arrays;
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
    default Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception {

        return component(componentName, actionName, input, connectionName, null);
    }

    /**
     * Invokes an action that reads cluster elements — an AI agent's chat above all, which resolves its model, tools,
     * memory and RAG from them.
     *
     * <p>
     * Elements are composed here rather than declared on the task because a code task orchestrates: it may call a
     * drafting model and then a reviewing one, or a cheap model with a fallback to a strong one, and one element set
     * per task would force each of those into its own task. What may not be written here is a connection — an element
     * names one of the task's declared connections, so the credential stays the platform's business and a name the task
     * never declared fails the call.
     *
     * <p>
     * The map is keyed by cluster element type ({@code model}, {@code tools}, {@code chatMemory}, {@code rag}, ...);
     * each value is one element, or a list of them for a type that takes several. An element is
     * {@code {type, connection?, name?, parameters?}}, where {@code type} is a
     * {@code <componentName>/v<version>/<elementName>} string. A tool's {@code name} is what the model calls it,
     * defaulting — like every element's name — to the element name in {@code type}.
     *
     * <p>
     * Nothing about an element is checked before the task runs, since elements are written in code rather than
     * declared: a wrong type or parameter name surfaces here, at call time.
     *
     * @param componentName   the name of the component that defines the action
     * @param actionName      the name of the action to invoke
     * @param input           the input parameters passed to the action, keyed by parameter name
     * @param connectionName  the name of the connection to use when invoking the action
     * @param clusterElements the elements to wire for this call, keyed by cluster element type; {@code null} or empty
     *                        for an action that reads none
     * @return the value produced by the invoked action, or {@code null} if it yields no result
     * @throws Exception if the action cannot be invoked or fails while executing
     */
    Object component(
        String componentName, String actionName, Map<String, ?> input, String connectionName,
        Map<String, ?> clusterElements) throws Exception;

    /**
     * Returns everything the workflow has produced up to this task: the workflow's inputs plus the output of every task
     * that already ran, keyed by task name.
     *
     * <p>
     * The map is a snapshot taken when this task was dispatched, so it never changes while the task runs, and a task
     * that has not run yet is simply absent.
     *
     * @return the workflow inputs and prior task outputs, keyed by name
     */
    Map<String, ?> input();

    /**
     * Returns one entry of {@link #input()} — the output of an earlier task, or a workflow input, by name.
     *
     * <p>
     * An unknown name fails rather than returning {@code null}, since in code a name that is not there is almost always
     * a typo or a task ordering mistake. Use {@code input().get(name)} when absence is a legitimate outcome.
     *
     * @param name the task or workflow input name
     * @return the value stored under that name
     * @throws IllegalArgumentException if nothing is stored under that name
     */
    Object input(String name);

    /**
     * Returns the task's own declared parameters, with any {@code ${...}} expression in them already evaluated against
     * the job context.
     *
     * <p>
     * These are the values the task declared in its definition, kept separate from {@link #input()} so a parameter is
     * never confused with another task's output — the two share no namespace.
     *
     * @return the task's parameters, keyed by name
     */
    Map<String, ?> parameters();

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
     * @param level   the severity level of the message
     * @param message the message to log
     */
    void log(LogLevel level, String message);

    /**
     * The severity levels a task can log at. Script tasks name a level as a string ({@code context.log("warn", ...)});
     * the engine maps it through {@link #of(String)} before the call reaches a {@link TaskContext}.
     */
    enum LogLevel {

        TRACE, DEBUG, INFO, WARN, ERROR;

        /**
         * Resolves a level by name, case-insensitively.
         *
         * @param level the level name, e.g. {@code "warn"}
         * @return the matching level
         * @throws IllegalArgumentException if the name does not match a level — a typo is reported where it is written
         *                                  rather than silently downgraded to {@link #INFO}
         */
        public static LogLevel of(String level) {
            for (LogLevel logLevel : values()) {
                if (logLevel.name()
                    .equalsIgnoreCase(level)) {

                    return logLevel;
                }
            }

            throw new IllegalArgumentException(
                "Unknown log level %s; expected one of %s".formatted(level, Arrays.toString(values())));
        }
    }
}
