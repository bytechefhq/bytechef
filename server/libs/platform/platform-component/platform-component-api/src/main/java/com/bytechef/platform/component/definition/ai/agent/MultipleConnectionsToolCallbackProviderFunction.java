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

package com.bytechef.platform.component.definition.ai.agent;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * Like {@link ToolCallbackProviderFunction} but receives the full component-connections map so that cluster elements
 * that need connections for multiple components (e.g. skillsTool) can pass them into downstream engines such as
 * {@code PolyglotEngine}.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface MultipleConnectionsToolCallbackProviderFunction extends BaseToolFunction {

    ToolCallbackProvider apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) throws Exception;
}
