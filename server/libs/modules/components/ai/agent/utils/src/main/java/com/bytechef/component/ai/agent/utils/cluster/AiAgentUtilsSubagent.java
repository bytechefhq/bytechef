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

package com.bytechef.component.ai.agent.utils.cluster;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ai.agent.SubagentFunction.SUBAGENT;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Property;

/**
 * A builder-defined subagent for the Task Tool. Its persona — name, description and instructions — is authored here
 * rather than shipped with the product, and its nested TOOLS children are the only tools it may call. Nothing grants a
 * subagent a capability the builder did not attach to it.
 *
 * @author Ivica Cardic
 */
public class AiAgentUtilsSubagent {

    public static final ClusterElementDefinition<?> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.clusterElement("subagent")
            .title("Subagent")
            .description("A specialized agent the Task Tool can delegate to.")
            .type(SUBAGENT)
            .properties(
                string("subagentName")
                    .label("Name")
                    .description("Identifier the parent agent uses when delegating to this subagent.")
                    .expressionEnabled(false)
                    .required(true),
                string("description")
                    .label("Description")
                    .description("When the parent agent should delegate to this subagent.")
                    .controlType(Property.ControlType.TEXT_AREA)
                    .expressionEnabled(false)
                    .required(true),
                string("instructions")
                    .label("Instructions")
                    .description("System prompt that tells this subagent how to do its work.")
                    .controlType(Property.ControlType.TEXT_AREA)
                    .expressionEnabled(false)
                    .required(true));

    private AiAgentUtilsSubagent() {
    }
}
