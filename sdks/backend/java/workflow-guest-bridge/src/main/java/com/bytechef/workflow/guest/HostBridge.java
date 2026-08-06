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
     * Returns the job context snapshot — the workflow's inputs plus every prior task's output — serialized as JSON.
     *
     * @return the workflow inputs and prior task outputs as a JSON object
     */
    String input();

    /**
     * Returns one entry of the job context snapshot by name, serialized as JSON. Resolution happens on the host so an
     * unknown name fails there, with the host's explanation of why it is missing.
     *
     * @param name the task or workflow input name
     * @return the value as JSON
     */
    String input(String name);

    /**
     * Returns the task's own declared parameters, serialized as JSON.
     *
     * @return the task's parameters as a JSON object
     */
    String parameters();

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
