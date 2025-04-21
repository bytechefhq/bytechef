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

package com.bytechef.component.mautic;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.mautic.action.MauticCreateCompanyAction;
import com.bytechef.component.mautic.action.MauticCreateContactAction;
import com.bytechef.component.mautic.action.MauticGetCompanyAction;
import com.bytechef.component.mautic.action.MauticGetContactAction;
import com.bytechef.component.mautic.connection.MauticConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nikolina Spehar
 */
@AutoService(ComponentHandler.class)
public class MauticComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mautic")
        .title("Mautic")
        .description(
            "Simplifying campaign design and optimization with a user-friendly" +
                " click-and-drag interface allowing you to build complex campaigns.")
        .icon("path:assets/mautic.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(MauticConnection.CONNECTION_DEFINITION)
        .actions(
            MauticCreateCompanyAction.ACTION_DEFINITION,
            MauticCreateContactAction.ACTION_DEFINITION,
            MauticGetCompanyAction.ACTION_DEFINITION,
            MauticGetContactAction.ACTION_DEFINITION)
        .clusterElements(
            tool(MauticCreateCompanyAction.ACTION_DEFINITION),
            tool(MauticCreateContactAction.ACTION_DEFINITION),
            tool(MauticGetCompanyAction.ACTION_DEFINITION),
            tool(MauticGetContactAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
