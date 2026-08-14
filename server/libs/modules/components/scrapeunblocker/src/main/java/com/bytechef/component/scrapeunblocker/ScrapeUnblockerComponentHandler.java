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

package com.bytechef.component.scrapeunblocker;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.scrapeunblocker.action.ScrapeUnblockerGetPageSourceAction;
import com.bytechef.component.scrapeunblocker.action.ScrapeUnblockerGetParsedDataAction;
import com.bytechef.component.scrapeunblocker.connection.ScrapeUnblockerConnection;
import com.google.auto.service.AutoService;

/**
 * @author Nerius Rutkauskas
 */
@AutoService(ComponentHandler.class)
public class ScrapeUnblockerComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("scrapeunblocker")
        .title("ScrapeUnblocker")
        .version(1)
        .description(
            "ScrapeUnblocker is a web scraping API that returns fully rendered HTML or AI-parsed JSON from any URL, " +
                "bypassing anti-bot protection such as Cloudflare, DataDome, PerimeterX and Akamai.")
        .icon("path:assets/scrapeunblocker.svg")
        .categories(ComponentCategory.ANALYTICS)
        .connection(ScrapeUnblockerConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("", "https://developers.scrapeunblocker.com")
        .actions(
            ScrapeUnblockerGetPageSourceAction.ACTION_DEFINITION,
            ScrapeUnblockerGetParsedDataAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ScrapeUnblockerGetPageSourceAction.ACTION_DEFINITION),
            tool(ScrapeUnblockerGetParsedDataAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
