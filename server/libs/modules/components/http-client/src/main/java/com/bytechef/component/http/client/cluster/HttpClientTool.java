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

package com.bytechef.component.http.client.cluster;

import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_DESCRIPTION;
import static com.bytechef.ai.tool.constant.ToolConstants.TOOL_NAME;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.definition.Property.ControlType.TEXT_AREA;

import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public class HttpClientTool {

    public static ModifiableClusterElementDefinition<ToolFunction> of(ActionDefinition actionDefinition) {
        ModifiableClusterElementDefinition<ToolFunction> clusterElementDefinition = tool(actionDefinition);

        List<Property> toolProperties = new ArrayList<>();

        toolProperties.add(
            string(TOOL_NAME)
                .label("Name")
                .description("The tool name exposed to the AI model.")
                .expressionEnabled(false)
                .required(true));
        toolProperties.add(
            string(TOOL_DESCRIPTION)
                .label("Description")
                .description("The tool description exposed to the AI model.")
                .controlType(TEXT_AREA)
                .expressionEnabled(false)
                .required(true));

        actionDefinition.getProperties()
            .ifPresent(toolProperties::addAll);

        return clusterElementDefinition.properties(toolProperties);
    }

    private HttpClientTool() {
    }
}
