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

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.api.Advisor;

/**
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface GuardrailsFunction {

    ClusterElementType GUARDRAILS = new ClusterElementType("GUARDRAILS", "guardrails", "Guardrails", true, false);

    /**
     * @param inputParameters      the input parameters for the guardrails configuration
     * @param connectionParameters the connection parameters
     * @param extensions           the extensions containing nested cluster elements
     * @param componentConnections the component connections map
     * @param context              the component invocation context
     * @return the guardrails advisor
     */
    Advisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections, Context context) throws Exception;
}
