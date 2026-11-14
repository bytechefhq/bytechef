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
 * @author Ivica Cardic
 */
public interface TaskContext {

    /**
     * Calls a component action without naming a connection.
     *
     * @param componentName the component name
     * @param actionName    the action name
     * @param input         the action input
     * @return the action output
     * @throws Exception when the action fails
     */
    default Object component(String componentName, String actionName, Map<String, ?> input) throws Exception {
        return component(componentName, actionName, input, null);
    }

    default Object component(String componentName, String actionName, Map<String, ?> input, String connectionName)
        throws Exception {

        return component(componentName, actionName, input, connectionName, null);
    }

    Object component(
        String componentName, String actionName, Map<String, ?> input, String connectionName,
        Map<String, ?> clusterElements) throws Exception;

    Map<String, ?> input();

    Object input(String name);

    Map<String, ?> parameters();

    Map<String, ?> connection(String connectionName) throws Exception;

    void log(LogLevel level, String message);

    enum LogLevel {

        TRACE, DEBUG, INFO, WARN, ERROR;

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
