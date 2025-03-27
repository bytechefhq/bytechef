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

package com.bytechef.component.google.sheets.cluster;

import static com.bytechef.component.definition.ai.agent.ToolFunction.TOOLS;
import static com.bytechef.component.google.sheets.action.GoogleSheetsCreateColumnAction.OUTPUT_SCHEMA;
import static com.bytechef.component.google.sheets.action.GoogleSheetsCreateColumnAction.PROPERTIES;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CREATE_COLUMN;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CREATE_COLUMN_DESCRIPTION;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.CREATE_COLUMN_TITLE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ai.agent.SingleConnectionToolFunction;
import com.bytechef.component.google.sheets.action.GoogleSheetsCreateColumnAction;

/**
 * @author Monika Kušter
 */
public class GoogleSheetsCreateColumnTool {

    public static final ClusterElementDefinition<SingleConnectionToolFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<SingleConnectionToolFunction>clusterElement(CREATE_COLUMN)
            .title(CREATE_COLUMN_TITLE)
            .description(CREATE_COLUMN_DESCRIPTION)
            .type(TOOLS)
            .properties(PROPERTIES)
            .output(OUTPUT_SCHEMA)
            .object(() -> GoogleSheetsCreateColumnAction::perform);
}
