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

package com.bytechef.component.baserow.cluster;

import static com.bytechef.component.baserow.action.BaserowCreateRowAction.PROPERTIES;
import static com.bytechef.component.baserow.constant.BaserowConstants.CREATE_ROW;
import static com.bytechef.component.baserow.constant.BaserowConstants.CREATE_ROW_DESCRIPTION;
import static com.bytechef.component.baserow.constant.BaserowConstants.CREATE_ROW_TITLE;
import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;

import com.bytechef.component.baserow.action.BaserowCreateRowAction;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;

/**
 * @author Monika Kušter
 */
public class BaserowCreateRowTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(CREATE_ROW)
            .title(CREATE_ROW_TITLE)
            .description(CREATE_ROW_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .output()
            .object(() -> BaserowCreateRowAction::perform);
}
