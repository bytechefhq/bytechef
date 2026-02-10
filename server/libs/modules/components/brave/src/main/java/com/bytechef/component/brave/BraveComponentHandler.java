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

package com.bytechef.component.brave;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.brave.action.BraveSearchAction;
import com.bytechef.component.brave.connection.BraveConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marko Krišković
 */
@AutoService(ComponentHandler.class)
public class BraveComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("brave")
        .title("Brave")
        .description(
            "Brave gives you access to the same powerful, independent search index that powers the privacy-first search engine trusted by millions")
        .icon("path:assets/brave.svg")
        .categories(ComponentCategory.MARKETING_AUTOMATION)
        .connection(BraveConnection.CONNECTION_DEFINITION)
        .actions(
            BraveSearchAction.ACTION_DEFINITION)
        .clusterElements(
            tool(BraveSearchAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
