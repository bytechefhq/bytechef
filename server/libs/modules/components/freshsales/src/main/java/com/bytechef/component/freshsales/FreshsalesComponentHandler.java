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

package com.bytechef.component.freshsales;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.freshsales.connection.FreshsalesConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.freshsales.action.FreshsalesCreateAccountAction;
import com.bytechef.component.freshsales.action.FreshsalesCreateContactAction;
import com.bytechef.component.freshsales.action.FreshsalesCreateLeadAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class FreshsalesComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("freshsales")
        .title("Freshsales")
        .description(
            "Freshsales is a customer relationship management (CRM) software designed to help businesses streamline " +
                "sales processes and manage customer interactions effectively.")
        .icon("path:assets/freshsales.svg")
        .categories(ComponentCategory.CRM)
        .connection(CONNECTION_DEFINITION)
        .actions(
            FreshsalesCreateAccountAction.ACTION_DEFINITION,
            FreshsalesCreateContactAction.ACTION_DEFINITION,
            FreshsalesCreateLeadAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
