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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.WebSocketHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;

/**
 * Variant of {@link com.bytechef.component.definition.ActionDefinition.WebSocketPerformFunction} for actions that
 * resolve multiple connections + cluster-element extensions (typically AI agents whose model / memory / tools are
 * supplied via cluster elements).
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface MultipleConnectionsWebSocketPerformFunction extends ActionDefinition.BasePerformFunction {

    /**
     * Execute the action and return a {@link WebSocketHandler} that handles the WS lifecycle.
     *
     * @param inputParameters      the input parameters for the action
     * @param componentConnections the parameters related to the connections
     * @param extensions           the parameters related to the extensions (cluster elements)
     * @param context              the context in which the action is executed
     * @return the {@link WebSocketHandler} that will own the WS connection for the duration of the task
     * @throws Exception if an error occurs during the execution of the action
     */
    WebSocketHandler apply(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) throws Exception;
}
