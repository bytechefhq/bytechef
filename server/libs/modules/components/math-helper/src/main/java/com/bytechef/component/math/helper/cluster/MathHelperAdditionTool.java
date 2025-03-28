/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.math.helper.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.math.helper.action.MathHelperAdditionAction.OUTPUT_SCHEMA;
import static com.bytechef.component.math.helper.action.MathHelperAdditionAction.PROPERTIES;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.ADDITION;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.ADDITION_DESCRIPTION;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.ADDITION_TITLE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.math.helper.action.MathHelperAdditionAction;

/**
 * @author Monika Kušter
 */
public class MathHelperAdditionTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(ADDITION)
            .title(ADDITION_TITLE)
            .description(ADDITION_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .output(OUTPUT_SCHEMA)
            .object(() -> MathHelperAdditionAction::perform);
}
