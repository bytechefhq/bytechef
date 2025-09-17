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

package com.bytechef.component.zenrows;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.zenrows.action.ZenRowScrapeUrlAction;
import com.bytechef.component.zenrows.action.ZenRowsScrapeUrlAutoparseAction;
import com.bytechef.component.zenrows.action.ZenRowsScrapeUrlWithCssSelectorAction;
import com.bytechef.component.zenrows.connection.ZenRowsConnection;
import com.google.auto.service.AutoService;

@AutoService(ComponentHandler.class)
public class ZenRowsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("zenrows")
        .title("ZenRows")
        .description(
            "ZenRows is a web scraping service with an advanced toolkit and APIs that simplify data extraction from " +
                "bot-protected websites.")
        .icon("path:assets/zenrows.svg")
        .categories(ComponentCategory.ANALYTICS)
        .connection(ZenRowsConnection.CONNECTION_DEFINITION)
        .actions(
            ZenRowScrapeUrlAction.ACTION_DEFINITION,
            ZenRowsScrapeUrlAutoparseAction.ACTION_DEFINITION,
            ZenRowsScrapeUrlWithCssSelectorAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ZenRowScrapeUrlAction.ACTION_DEFINITION),
            tool(ZenRowsScrapeUrlAutoparseAction.ACTION_DEFINITION),
            tool(ZenRowsScrapeUrlWithCssSelectorAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
