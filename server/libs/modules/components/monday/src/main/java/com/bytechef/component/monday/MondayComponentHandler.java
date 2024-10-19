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

package com.bytechef.component.monday;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.monday.constant.MondayConstants.MONDAY;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.monday.action.MondayCreateBoardAction;
import com.bytechef.component.monday.action.MondayCreateColumnAction;
import com.bytechef.component.monday.action.MondayCreateGroupAction;
import com.bytechef.component.monday.action.MondayCreateItemAction;
import com.bytechef.component.monday.connection.MondayConnection;
import com.bytechef.component.monday.trigger.MondayNewItemInBoardTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class MondayComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MONDAY)
        .title("monday.com")
        .description(
            "Monday.com is a work operating system that powers teams to run projects and workflows with confidence.")
        .connection(MondayConnection.CONNECTION_DEFINITION)
        .icon("path:assets/monday.svg")
        .actions(
            MondayCreateColumnAction.ACTION_DEFINITION,
            MondayCreateGroupAction.ACTION_DEFINITION,
            MondayCreateItemAction.ACTION_DEFINITION,
            MondayCreateBoardAction.ACTION_DEFINITION)
        .triggers(MondayNewItemInBoardTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
