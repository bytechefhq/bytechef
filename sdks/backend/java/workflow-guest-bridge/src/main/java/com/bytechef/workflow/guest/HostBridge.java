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

package com.bytechef.workflow.guest;

/**
 * Guest-side view of the host callback object a code workflow loader places into the polyglot bindings. Method for
 * method it must match the {@code @HostAccess.Export} methods of the host's {@code CodeWorkflowHostBridge} — that
 * exported set is the entire sandbox boundary between guest code workflow code and the host JVM.
 *
 * @author Ivica Cardic
 */
public interface HostBridge {

    /**
     * Invokes a component action on the host.
     *
     * @param requestJson a JSON object {@code {componentName, actionName, input, connectionName}}
     * @return the action result serialized as JSON
     */
    String componentExecute(String requestJson);

    /**
     * Returns a wired connection's parameters, serialized as JSON.
     *
     * @param connectionName the name of the connection, as declared on the task
     * @return the connection's parameters as a JSON object
     */
    String connection(String connectionName);

    /**
     * Logs a message on the host.
     *
     * @param level   the severity level of the message
     * @param message the message to log
     */
    void log(String level, String message);
}
