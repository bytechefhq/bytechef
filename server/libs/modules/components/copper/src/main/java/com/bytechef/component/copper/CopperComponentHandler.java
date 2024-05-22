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

package com.bytechef.component.copper;

import static com.bytechef.component.copper.constant.CopperConstants.COPPER;
import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.copper.action.CopperCreateActivityAction;
import com.bytechef.component.copper.action.CopperCreateCompanyAction;
import com.bytechef.component.copper.action.CopperCreatePersonAction;
import com.bytechef.component.copper.connection.CopperConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class CopperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(COPPER)
        .title("Copper")
        .description(
            "Copper is a customer relationship management (CRM) software designed to streamline and optimize sales " +
                "processes, providing tools for managing contact, leads, opportunities, and communications in one " +
                "centralized platform.")
        .icon("path:assets/copper.svg")
        .categories(ComponentCategory.CRM)
        .connection(CopperConnection.CONNECTION_DEFINITION)
        .actions(
            CopperCreateActivityAction.ACTION_DEFINITION,
            CopperCreateCompanyAction.ACTION_DEFINITION,
            CopperCreatePersonAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
