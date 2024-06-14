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

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.zoho.connection.ZohoCrmConnection.CONNECTION_DEFINITION;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.ZOHO_CRM;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zoho.action.ZohoCrmCreateUserAction;
import com.bytechef.component.zoho.action.ZohoCrmGetAllUsersAction;
import com.bytechef.component.zoho.action.ZohoCrmGetOrganizationData;
import com.google.auto.service.AutoService;

/**
 * @author Luka Ljubić
 */
@AutoService(ComponentHandler.class)
public class ZohoCrmComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(ZOHO_CRM)
        .title("Zoho CRM")
        .description(
            "Zoho CRM acts as a single repository to bring your sales, marketing, and customer support activities together, and streamline your process, policy, and people ...")
        .categories(ComponentCategory.CRM)
        .connection(CONNECTION_DEFINITION)
        .actions(
            ZohoCrmGetAllUsersAction.ACTION_DEFINITION,
            ZohoCrmCreateUserAction.ACTION_DEFINITION,
            ZohoCrmGetOrganizationData.ACTION_DEFINITION)
        .icon("path:assets/zoho.svg");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
