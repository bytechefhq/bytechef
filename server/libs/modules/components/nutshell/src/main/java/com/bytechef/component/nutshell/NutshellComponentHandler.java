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

package com.bytechef.component.nutshell;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.nutshell.action.NutshellCreateCompanyAction;
import com.bytechef.component.nutshell.action.NutshellCreateContactAction;
import com.bytechef.component.nutshell.action.NutshellCreateLeadAction;
import com.bytechef.component.nutshell.connection.NutshellConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class NutshellComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("nutshell")
        .title("Nutshell")
        .description(
            "Nutshell CRM is a user-friendly customer relationship management software designed to help " +
                "small businesses manage sales, track leads, and streamline communication.")
        .customAction(true)
        .icon("path:assets/nutshell.svg")
        .categories(ComponentCategory.CRM)
        .connection(NutshellConnection.CONNECTION_DEFINITION)
        .actions(
            NutshellCreateContactAction.ACTION_DEFINITION,
            NutshellCreateCompanyAction.ACTION_DEFINITION,
            NutshellCreateLeadAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
