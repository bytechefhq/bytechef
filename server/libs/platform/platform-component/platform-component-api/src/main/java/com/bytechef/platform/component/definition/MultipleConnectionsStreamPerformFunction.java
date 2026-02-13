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
import com.bytechef.component.definition.ActionDefinition.SseEmitterHandler;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface MultipleConnectionsStreamPerformFunction extends ActionDefinition.BasePerformFunction {

    /**
     * Execute the action and return an {@link SseEmitterHandler} used to stream events.
     *
     * @param inputParameters      the input parameters for the action
     * @param componentConnections the parameters related to the connections
     * @param extensions           the parameters related to the extensions
     * @param context              the context in which the action is executed
     * @return the {@link SseEmitterHandler} that will stream events for the duration of this action
     * @throws Exception if an error occurs during the execution of the action
     */
    SseEmitterHandler apply(
        Parameters inputParameters, Map<String, ComponentConnection> componentConnections, Parameters extensions,
        ActionContext context) throws Exception;
}
