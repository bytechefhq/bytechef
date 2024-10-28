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

package com.bytechef.component.zoho;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.zoho.connection.ZohoCrmConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zoho.action.ZohoCrmAddUserAction;
import com.bytechef.component.zoho.action.ZohoCrmGetOrganizationAction;
import com.bytechef.component.zoho.action.ZohoCrmListUsersAction;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class ZohoCrmComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("zohoCrm")
        .title("Zoho CRM")
        .description(
            "Zoho CRM is a cloud-based customer relationship management platform that integrates sales, marketing, " +
                "and customer support activities to streamline business processes and enhance team.")
        .categories(ComponentCategory.CRM)
        .connection(CONNECTION_DEFINITION)
        .actions(
            ZohoCrmAddUserAction.ACTION_DEFINITION,
            ZohoCrmGetOrganizationAction.ACTION_DEFINITION,
            ZohoCrmListUsersAction.ACTION_DEFINITION)
        .icon("path:assets/zoho-crm.svg");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
