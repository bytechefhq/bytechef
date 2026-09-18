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

package com.bytechef.component.neon;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.neon.action.NeonCreateRowAction;
import com.bytechef.component.neon.action.NeonDeleteRowAction;
import com.bytechef.component.neon.action.NeonGenerateServiceAccountAction;
import com.bytechef.component.neon.action.NeonListRowsAction;
import com.bytechef.component.neon.action.NeonUpdateRowAction;
import com.bytechef.component.neon.connection.NeonConnection;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class NeonComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("neon")
        .title("Neon")
        .description(
            "Neon is a serverless Postgres platform. Query, insert, update and delete rows in your database " +
                "through its Data API.")
        .icon("path:assets/neon.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(NeonConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("Neon Data API docs", "https://neon.com/docs/data-api/get-started")
        .actions(
            NeonGenerateServiceAccountAction.ACTION_DEFINITION,
            NeonListRowsAction.ACTION_DEFINITION,
            NeonCreateRowAction.ACTION_DEFINITION,
            NeonUpdateRowAction.ACTION_DEFINITION,
            NeonDeleteRowAction.ACTION_DEFINITION)
        .clusterElements(
            tool(NeonGenerateServiceAccountAction.ACTION_DEFINITION),
            tool(NeonListRowsAction.ACTION_DEFINITION),
            tool(NeonCreateRowAction.ACTION_DEFINITION),
            tool(NeonUpdateRowAction.ACTION_DEFINITION),
            tool(NeonDeleteRowAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
