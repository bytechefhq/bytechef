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

package com.bytechef.component.microsoft.dynamics.crm;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.dynamics.crm.action.MicrosoftDynamicsCrmCreateRecordAction;
import com.bytechef.component.microsoft.dynamics.crm.action.MicrosoftDynamicsCrmDeleteRecordAction;
import com.bytechef.component.microsoft.dynamics.crm.action.MicrosoftDynamicsCrmGetRecordAction;
import com.bytechef.component.microsoft.dynamics.crm.action.MicrosoftDynamicsCrmUpdateRecordAction;
import com.bytechef.component.microsoft.dynamics.crm.connection.MicrosoftDynamicsCrmConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftDynamicsCrmComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("microsoftDynamicsCrm")
        .title("Microsoft Dynamics CRM")
        .description(
            "Microsoft Dynamics CRM is a customer relationship management software that helps businesses " +
                "manage customer interactions, sales, marketing, and customer service processes efficiently.")
        .icon("path:assets/microsoft-dynamics-crm.svg")
        .categories(ComponentCategory.CRM)
        .connection(MicrosoftDynamicsCrmConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("", "https://learn.microsoft.com/en-us/power-apps/developer/data-platform/webapi/overview")
        .actions(
            MicrosoftDynamicsCrmCreateRecordAction.ACTION_DEFINITION,
            MicrosoftDynamicsCrmDeleteRecordAction.ACTION_DEFINITION,
            MicrosoftDynamicsCrmGetRecordAction.ACTION_DEFINITION,
            MicrosoftDynamicsCrmUpdateRecordAction.ACTION_DEFINITION)
        .clusterElements(
            tool(MicrosoftDynamicsCrmCreateRecordAction.ACTION_DEFINITION),
            tool(MicrosoftDynamicsCrmDeleteRecordAction.ACTION_DEFINITION),
            tool(MicrosoftDynamicsCrmGetRecordAction.ACTION_DEFINITION),
            tool(MicrosoftDynamicsCrmUpdateRecordAction.ACTION_DEFINITION))
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
