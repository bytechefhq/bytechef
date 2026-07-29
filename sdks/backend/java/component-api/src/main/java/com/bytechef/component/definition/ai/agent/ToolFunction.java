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

package com.bytechef.component.definition.ai.agent;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;

/**
 * Implements the behavior of a tool that an AI agent can invoke, executing an action against a provider using the
 * supplied parameters and returning a result the agent can consume.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface ToolFunction extends BaseToolFunction {

    /**
     * Executes the tool with the given parameters.
     *
     * @param inputParameters      the input parameters the agent supplied for the tool call
     * @param connectionParameters the connection parameters
     * @param context              the cluster element execution context
     * @return the result of the tool invocation, returned to the agent
     * @throws Exception if the tool invocation fails
     */
    Object apply(Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context)
        throws Exception;
}
