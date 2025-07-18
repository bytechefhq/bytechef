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

package com.bytechef.component.retable;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.retable.action.RetableDeleteRowAction;
import com.bytechef.component.retable.action.RetableInsertRowAction;
import com.bytechef.component.retable.action.RetableUpdateRowAction;
import com.bytechef.component.retable.connection.RetableConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */

@AutoService(ComponentHandler.class)
public class RetableComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("retable")
        .title("Retable")
        .description(
            "Retable is a online database and spreadsheet platform designed for teams and individuals who want to " +
                "organize, manage, and collaborate on structured data.")
        .icon("path:assets/retable.svg")
        .customAction(true)
        .categories(ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(RetableConnection.CONNECTION_DEFINITION)
        .actions(
            RetableDeleteRowAction.ACTION_DEFINITION,
            RetableInsertRowAction.ACTION_DEFINITION,
            RetableUpdateRowAction.ACTION_DEFINITION)
        .clusterElements(
            tool(RetableDeleteRowAction.ACTION_DEFINITION),
            tool(RetableInsertRowAction.ACTION_DEFINITION),
            tool(RetableUpdateRowAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
