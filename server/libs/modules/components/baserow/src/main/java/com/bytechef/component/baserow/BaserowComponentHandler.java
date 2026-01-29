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

package com.bytechef.component.baserow;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.baserow.action.BaserowCreateRowAction;
import com.bytechef.component.baserow.action.BaserowDeleteRowAction;
import com.bytechef.component.baserow.action.BaserowGetRowAction;
import com.bytechef.component.baserow.action.BaserowListRowsAction;
import com.bytechef.component.baserow.action.BaserowUpdateRowAction;
import com.bytechef.component.baserow.connection.BaserowConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class BaserowComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("baserow")
        .title("Baserow")
        .description(
            "Baserow is an open-source, no-code database platform that enables users to create, manage, and " +
                "collaborate on databases through a user-friendly interface.")
        .customAction(true)
        .customActionHelp("Baserow API docs", "https://baserow.io/api-docs")
        .icon("path:assets/baserow.svg")
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(BaserowConnection.CONNECTION_DEFINITION)
        .actions(
            BaserowCreateRowAction.ACTION_DEFINITION,
            BaserowDeleteRowAction.ACTION_DEFINITION,
            BaserowGetRowAction.ACTION_DEFINITION,
            BaserowListRowsAction.ACTION_DEFINITION,
            BaserowUpdateRowAction.ACTION_DEFINITION)
        .clusterElements(
            tool(BaserowCreateRowAction.ACTION_DEFINITION),
            tool(BaserowDeleteRowAction.ACTION_DEFINITION),
            tool(BaserowGetRowAction.ACTION_DEFINITION),
            tool(BaserowListRowsAction.ACTION_DEFINITION),
            tool(BaserowUpdateRowAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
