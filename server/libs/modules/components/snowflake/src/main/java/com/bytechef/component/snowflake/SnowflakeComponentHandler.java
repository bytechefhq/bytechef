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

package com.bytechef.component.snowflake;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.snowflake.action.SnowflakeDeleteRowAction;
import com.bytechef.component.snowflake.action.SnowflakeExecuteSqlAction;
import com.bytechef.component.snowflake.action.SnowflakeInsertRowAction;
import com.bytechef.component.snowflake.action.SnowflakeUpdateRowAction;
import com.bytechef.component.snowflake.connection.SnowflakeConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class SnowflakeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("snowflake")
        .title("Snowflake")
        .description(
            "Snowflake enables organizations to collaborate, build AI-powered data apps, and unlock data " +
                "insights—all within a secure and scalable AI Data Cloud.")
        .icon("path:assets/snowflake.svg")
        .categories(ComponentCategory.ANALYTICS)
        .connection(SnowflakeConnection.CONNECTION_DEFINITION)
        .actions(
            SnowflakeDeleteRowAction.ACTION_DEFINITION,
            SnowflakeExecuteSqlAction.ACTION_DEFINITION,
            SnowflakeInsertRowAction.ACTION_DEFINITION,
            SnowflakeUpdateRowAction.ACTION_DEFINITION)
        .clusterElements(
            tool(SnowflakeDeleteRowAction.ACTION_DEFINITION),
            tool(SnowflakeExecuteSqlAction.ACTION_DEFINITION),
            tool(SnowflakeInsertRowAction.ACTION_DEFINITION),
            tool(SnowflakeUpdateRowAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
