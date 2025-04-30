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

package com.bytechef.component.bamboohr;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.bamboohr.action.BambooHrCreateEmployeeAction;
import com.bytechef.component.bamboohr.action.BambooHrGetEmployeeAction;
import com.bytechef.component.bamboohr.action.BambooHrUpdateEmployeeAction;
import com.bytechef.component.bamboohr.action.BambooHrUpdateEmployeeFileAction;
import com.bytechef.component.bamboohr.connection.BambooHrConnection;
import com.bytechef.component.bamboohr.trigger.BambooHrNewEmployeeTrigger;
import com.bytechef.component.bamboohr.trigger.BambooHrUpdatedEmployeeTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marija Horvat
 */
@AutoService(ComponentHandler.class)
public class BambooHrComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("bambooHr")
        .title("BambooHR")
        .description(
            "BambooHR is a human resources software that helps HR teams manage employee data, hiring, onboarding, " +
                "time tracking, payroll, performance management, and more in one platform.")
        .icon("path:assets/bamboohr.svg")
        .categories(ComponentCategory.HRIS)
        .connection(BambooHrConnection.CONNECTION_DEFINITION)
        .actions(
            BambooHrCreateEmployeeAction.ACTION_DEFINITION,
            BambooHrUpdateEmployeeAction.ACTION_DEFINITION,
            BambooHrGetEmployeeAction.ACTION_DEFINITION,
            BambooHrUpdateEmployeeFileAction.ACTION_DEFINITION)
        .triggers(
            BambooHrUpdatedEmployeeTrigger.TRIGGER_DEFINITION,
            BambooHrNewEmployeeTrigger.TRIGGER_DEFINITION)
        .clusterElements(
            tool(BambooHrCreateEmployeeAction.ACTION_DEFINITION),
            tool(BambooHrUpdateEmployeeAction.ACTION_DEFINITION),
            tool(BambooHrGetEmployeeAction.ACTION_DEFINITION),
            tool(BambooHrUpdateEmployeeFileAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
