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

package com.bytechef.component.nocodb;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.nocodb.action.NocoDbCreateRecords;
import com.bytechef.component.nocodb.action.NocoDbDeleteRecords;
import com.bytechef.component.nocodb.action.NocoDbGetRecord;
import com.bytechef.component.nocodb.action.NocoDbSearchRecords;
import com.bytechef.component.nocodb.action.NocoDbUpdateRecords;
import com.bytechef.component.nocodb.connection.NocoDbConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class NocoDbComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("nocoDb")
        .title("NocoDB")
        .description(
            "NocoDB is an open-source platform that transforms databases into smart spreadsheets, enabling users " +
                "to manage and collaborate on data with a no-code interface.")
        .icon("path:assets/nocodb.svg")
        .categories(ComponentCategory.FILE_STORAGE)
        .connection(NocoDbConnection.CONNECTION_DEFINITION)
        .actions(
            NocoDbCreateRecords.ACTION_DEFINITION,
            NocoDbDeleteRecords.ACTION_DEFINITION,
            NocoDbGetRecord.ACTION_DEFINITION,
            NocoDbSearchRecords.ACTION_DEFINITION,
            NocoDbUpdateRecords.ACTION_DEFINITION)
        .clusterElements(
            tool(NocoDbCreateRecords.ACTION_DEFINITION),
            tool(NocoDbDeleteRecords.ACTION_DEFINITION),
            tool(NocoDbGetRecord.ACTION_DEFINITION),
            tool(NocoDbSearchRecords.ACTION_DEFINITION),
            tool(NocoDbUpdateRecords.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
