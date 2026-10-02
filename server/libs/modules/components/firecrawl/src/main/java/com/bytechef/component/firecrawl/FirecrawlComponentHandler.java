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

package com.bytechef.component.firecrawl;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.firecrawl.action.FirecrawlCrawlAction;
import com.bytechef.component.firecrawl.action.FirecrawlCreateBrowserSessionAction;
import com.bytechef.component.firecrawl.action.FirecrawlDeleteBrowserSessionAction;
import com.bytechef.component.firecrawl.action.FirecrawlExecuteBrowserCodeAction;
import com.bytechef.component.firecrawl.action.FirecrawlGetCrawlStatusAction;
import com.bytechef.component.firecrawl.action.FirecrawlInteractAction;
import com.bytechef.component.firecrawl.action.FirecrawlListBrowserSessionsAction;
import com.bytechef.component.firecrawl.action.FirecrawlMapAction;
import com.bytechef.component.firecrawl.action.FirecrawlScrapeAction;
import com.bytechef.component.firecrawl.action.FirecrawlSearchAction;
import com.bytechef.component.firecrawl.action.FirecrawlStopInteractAction;
import com.bytechef.component.firecrawl.connection.FirecrawlConnection;
import com.google.auto.service.AutoService;

/**
 * @author Marko Krišković
 */
@AutoService(ComponentHandler.class)
public class FirecrawlComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("firecrawl")
        .title("Firecrawl")
        .description("Firecrawl allows you to turn entire websites into LLM-ready markdown")
        .icon("path:assets/firecrawl.svg")
        .categories(ComponentCategory.HELPERS, ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(FirecrawlConnection.CONNECTION_DEFINITION)
        .actions(
            FirecrawlCrawlAction.ACTION_DEFINITION,
            FirecrawlCreateBrowserSessionAction.ACTION_DEFINITION,
            FirecrawlDeleteBrowserSessionAction.ACTION_DEFINITION,
            FirecrawlExecuteBrowserCodeAction.ACTION_DEFINITION,
            FirecrawlGetCrawlStatusAction.ACTION_DEFINITION,
            FirecrawlInteractAction.ACTION_DEFINITION,
            FirecrawlListBrowserSessionsAction.ACTION_DEFINITION,
            FirecrawlMapAction.ACTION_DEFINITION,
            FirecrawlScrapeAction.ACTION_DEFINITION,
            FirecrawlSearchAction.ACTION_DEFINITION,
            FirecrawlStopInteractAction.ACTION_DEFINITION)
        .clusterElements(
            tool(FirecrawlCrawlAction.ACTION_DEFINITION),
            tool(FirecrawlCreateBrowserSessionAction.ACTION_DEFINITION),
            tool(FirecrawlDeleteBrowserSessionAction.ACTION_DEFINITION),
            tool(FirecrawlExecuteBrowserCodeAction.ACTION_DEFINITION),
            tool(FirecrawlGetCrawlStatusAction.ACTION_DEFINITION),
            tool(FirecrawlInteractAction.ACTION_DEFINITION),
            tool(FirecrawlListBrowserSessionsAction.ACTION_DEFINITION),
            tool(FirecrawlMapAction.ACTION_DEFINITION),
            tool(FirecrawlScrapeAction.ACTION_DEFINITION),
            tool(FirecrawlSearchAction.ACTION_DEFINITION),
            tool(FirecrawlStopInteractAction.ACTION_DEFINITION))
        .customAction(true)
        .customActionHelp("Firecrawl API", "https://docs.firecrawl.dev/api-reference/introduction")
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
