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

package com.bytechef.component.notion;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.notion.action.NotionAddBlockToPageAction;
import com.bytechef.component.notion.action.NotionCreateDatabaseItemAction;
import com.bytechef.component.notion.action.NotionCreatePageAction;
import com.bytechef.component.notion.action.NotionGetDatabaseAction;
import com.bytechef.component.notion.action.NotionGetPageAction;
import com.bytechef.component.notion.action.NotionGetPageOrBlockChildrenAction;
import com.bytechef.component.notion.action.NotionListDatabaseItemsAction;
import com.bytechef.component.notion.action.NotionUpdateDatabaseItemAction;
import com.bytechef.component.notion.connection.NotionConnection;
import com.bytechef.component.notion.trigger.NotionNewDatabaseItemTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class NotionComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("notion")
        .title("Notion")
        .description("Notion is an all-in-one workspace for notes, tasks, wikis, and databases.")
        .connection(NotionConnection.CONNECTION_DEFINITION)
        .icon("path:assets/notion.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .customAction(true)
        .actions(
            NotionAddBlockToPageAction.ACTION_DEFINITION,
            NotionCreateDatabaseItemAction.ACTION_DEFINITION,
            NotionCreatePageAction.ACTION_DEFINITION,
            NotionGetDatabaseAction.ACTION_DEFINITION,
            NotionGetPageAction.ACTION_DEFINITION,
            NotionGetPageOrBlockChildrenAction.ACTION_DEFINITION,
            NotionListDatabaseItemsAction.ACTION_DEFINITION,
            NotionUpdateDatabaseItemAction.ACTION_DEFINITION)
        .clusterElements(
            tool(NotionAddBlockToPageAction.ACTION_DEFINITION),
            tool(NotionCreateDatabaseItemAction.ACTION_DEFINITION),
            tool(NotionCreatePageAction.ACTION_DEFINITION),
            tool(NotionGetDatabaseAction.ACTION_DEFINITION),
            tool(NotionGetPageAction.ACTION_DEFINITION),
            tool(NotionUpdateDatabaseItemAction.ACTION_DEFINITION))
        .triggers(NotionNewDatabaseItemTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
