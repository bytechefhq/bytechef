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

package com.bytechef.component.google.mail.aiagent;

import static com.bytechef.component.definition.aiagent.ToolFunction.TOOL;
import static com.bytechef.component.google.mail.action.GoogleMailSearchEmailAction.OUTPUT_SCHEMA;
import static com.bytechef.component.google.mail.action.GoogleMailSearchEmailAction.PROPERTIES;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.aiagent.ToolFunction;
import com.bytechef.component.google.mail.action.GoogleMailSearchEmailAction;

/**
 * @author Ivica Cardic
 */
public class GoogleMailSearchEmailTool {

    public static final ClusterElementDefinition<ToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ToolFunction>clusterElement("searchEmail")
            .title("Search Email")
            .description("Search email to the specified email address.")
            .type(TOOL)
            .properties(PROPERTIES)
            .output(OUTPUT_SCHEMA)
            .object(() -> GoogleMailSearchEmailAction::perform);
}
