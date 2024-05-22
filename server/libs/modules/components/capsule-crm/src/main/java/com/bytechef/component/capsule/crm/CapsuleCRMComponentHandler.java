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

package com.bytechef.component.capsule.crm;

import static com.bytechef.component.capsule.crm.connection.CapsuleCRMConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.capsule.crm.constant.CapsuleCRMConstants.CAPSULE_CRM;
import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.capsule.crm.action.CapsuleCRMCreateContactAction;
import com.bytechef.component.capsule.crm.action.CapsuleCRMCreateTaskAction;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class CapsuleCRMComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(CAPSULE_CRM)
        .title("Capsule CRM")
        .description(
            "Capsule CRM is a cloud-based customer relationship management platform designed to help businesses " +
                "manage contacts, track sales opportunities, and collaborate with their teams efficiently.")
        .icon("path:assets/capsule-crm.svg")
        .connection(CONNECTION_DEFINITION)
        .categories(ComponentCategory.CRM)
        .actions(
            CapsuleCRMCreateContactAction.ACTION_DEFINITION,
            CapsuleCRMCreateTaskAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
