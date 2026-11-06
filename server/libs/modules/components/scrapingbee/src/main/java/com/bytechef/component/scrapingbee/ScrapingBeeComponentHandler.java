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

package com.bytechef.component.scrapingbee;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.scrapingbee.action.ScrapingBeeAiExtractAction;
import com.bytechef.component.scrapingbee.action.ScrapingBeeExtractDataAction;
import com.bytechef.component.scrapingbee.action.ScrapingBeeGetUsageAction;
import com.bytechef.component.scrapingbee.action.ScrapingBeeGoogleSearchAction;
import com.bytechef.component.scrapingbee.action.ScrapingBeeScrapeUrlAction;
import com.bytechef.component.scrapingbee.action.ScrapingBeeTakeScreenshotAction;
import com.bytechef.component.scrapingbee.connection.ScrapingBeeConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ScrapingBeeComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("scrapingbee")
        .title("ScrapingBee")
        .version(1)
        .description(
            "ScrapingBee is a web scraping API that renders pages in headless browsers and rotates proxies, " +
                "returning HTML, Markdown, screenshots, CSS or AI-extracted data, and Google search results.")
        .icon("path:assets/scrapingbee.svg")
        .categories(ComponentCategory.HELPERS, ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(ScrapingBeeConnection.CONNECTION_DEFINITION)
        .customAction(true)
        .customActionHelp("", "https://www.scrapingbee.com/documentation/")
        .actions(
            ScrapingBeeScrapeUrlAction.ACTION_DEFINITION,
            ScrapingBeeExtractDataAction.ACTION_DEFINITION,
            ScrapingBeeAiExtractAction.ACTION_DEFINITION,
            ScrapingBeeTakeScreenshotAction.ACTION_DEFINITION,
            ScrapingBeeGoogleSearchAction.ACTION_DEFINITION,
            ScrapingBeeGetUsageAction.ACTION_DEFINITION)
        .clusterElements(
            tool(ScrapingBeeScrapeUrlAction.ACTION_DEFINITION),
            tool(ScrapingBeeExtractDataAction.ACTION_DEFINITION),
            tool(ScrapingBeeAiExtractAction.ACTION_DEFINITION),
            tool(ScrapingBeeTakeScreenshotAction.ACTION_DEFINITION),
            tool(ScrapingBeeGoogleSearchAction.ACTION_DEFINITION),
            tool(ScrapingBeeGetUsageAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
