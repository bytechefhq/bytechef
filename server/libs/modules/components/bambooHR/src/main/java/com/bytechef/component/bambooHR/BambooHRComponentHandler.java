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

package com.bytechef.component.bambooHR;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.bambooHR.action.BambooHRCreateEmployeeAction;
import com.bytechef.component.bambooHR.action.BambooHRGetEmployeeAction;
import com.bytechef.component.bambooHR.action.BambooHRUpdateEmployeeAction;
import com.bytechef.component.bambooHR.action.BambooHRUpdateEmployeeFileAction;
import com.bytechef.component.bambooHR.connection.BambooHRConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class BambooHRComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("bambooHR")
        .title("BambooHR")
        .description("BambooHR is a Human Resources software that helps HR teams manage employee data, " +
            "hiring, onboarding, time tracking, payroll, performance management, and more in one platform.")
        .icon("path:assets/bambooHR.svg")
        .categories(ComponentCategory.CRM)
        .connection(BambooHRConnection.CONNECTION_DEFINITION)
        .actions(
            BambooHRCreateEmployeeAction.ACTION_DEFINITION,
            BambooHRUpdateEmployeeAction.ACTION_DEFINITION,
            BambooHRGetEmployeeAction.ACTION_DEFINITION,
            BambooHRUpdateEmployeeFileAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
