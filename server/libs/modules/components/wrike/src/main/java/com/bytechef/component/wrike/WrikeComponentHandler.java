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

package com.bytechef.component.wrike;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.wrike.action.WrikeCreateCommentAction;
import com.bytechef.component.wrike.action.WrikeCreateFolderAction;
import com.bytechef.component.wrike.action.WrikeCreateProjectAction;
import com.bytechef.component.wrike.action.WrikeCreateTaskAction;
import com.bytechef.component.wrike.connection.WrikeConnection;
import com.bytechef.component.wrike.trigger.WrikeNewTaskTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class WrikeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("wrike")
        .title("Wrike")
        .description("Wrike's Powerful Project Management Software Provides Users with Enterprise-Level Security.")
        .icon("path:assets/wrike.svg")
        .categories(ComponentCategory.PROJECT_MANAGEMENT)
        .connection(WrikeConnection.CONNECTION_DEFINITION)
        .actions(
            WrikeCreateCommentAction.ACTION_DEFINITION,
            WrikeCreateFolderAction.ACTION_DEFINITION,
            WrikeCreateProjectAction.ACTION_DEFINITION,
            WrikeCreateTaskAction.ACTION_DEFINITION)
        .clusterElements(
            tool(WrikeCreateCommentAction.ACTION_DEFINITION),
            tool(WrikeCreateFolderAction.ACTION_DEFINITION),
            tool(WrikeCreateProjectAction.ACTION_DEFINITION),
            tool(WrikeCreateTaskAction.ACTION_DEFINITION))
        .triggers(WrikeNewTaskTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
