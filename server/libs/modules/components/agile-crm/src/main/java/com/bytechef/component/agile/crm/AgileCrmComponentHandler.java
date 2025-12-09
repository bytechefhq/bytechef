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

package com.bytechef.component.agile.crm;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.agile.crm.action.AgileCrmCreateContactAction;
import com.bytechef.component.agile.crm.action.AgileCrmCreateDealAction;
import com.bytechef.component.agile.crm.action.AgileCrmCreateTaskAction;
import com.bytechef.component.agile.crm.connection.AgileCrmConnection;
import com.bytechef.component.agile.crm.trigger.AgileCrmNewTaskTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class AgileCrmComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("agileCrm")
        .title("Agile CRM")
        .description(
            "All-in-One CRM. Automate your sales, marketing, and service in one platform. Avoid data leaks and " +
                "enable consistent messaging.")
        .icon("path:assets/agile-crm.svg")
        .categories(ComponentCategory.CRM)
        .connection(AgileCrmConnection.CONNECTION_DEFINITION)
        .actions(
            AgileCrmCreateContactAction.ACTION_DEFINITION,
            AgileCrmCreateDealAction.ACTION_DEFINITION,
            AgileCrmCreateTaskAction.ACTION_DEFINITION)
        .clusterElements(
            tool(AgileCrmCreateContactAction.ACTION_DEFINITION),
            tool(AgileCrmCreateDealAction.ACTION_DEFINITION),
            tool(AgileCrmCreateTaskAction.ACTION_DEFINITION))
        .triggers(AgileCrmNewTaskTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
