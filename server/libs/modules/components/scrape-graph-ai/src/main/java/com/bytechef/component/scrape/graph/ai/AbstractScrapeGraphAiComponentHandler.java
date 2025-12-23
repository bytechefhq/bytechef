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

package com.bytechef.component.scrape.graph.ai;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.scrape.graph.ai.action.ScrapeGraphAiGetCrawlStatusAction;
import com.bytechef.component.scrape.graph.ai.action.ScrapeGraphAiMarkdownifyAction;
import com.bytechef.component.scrape.graph.ai.action.ScrapeGraphAiSearchScraperAction;
import com.bytechef.component.scrape.graph.ai.action.ScrapeGraphAiSmartScraperAction;
import com.bytechef.component.scrape.graph.ai.action.ScrapeGraphAiStartCrawlAction;
import com.bytechef.component.scrape.graph.ai.connection.ScrapeGraphAiConnection;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractScrapeGraphAiComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("scrapeGraphAi")
            .title("ScrapeGraphAI")
            .description(
                "ScrapeGraphAI is a web scraping python library that uses LLM and direct graph logic to create scraping pipelines for websites and local documents."))
                    .actions(modifyActions(ScrapeGraphAiSearchScraperAction.ACTION_DEFINITION,
                        ScrapeGraphAiMarkdownifyAction.ACTION_DEFINITION,
                        ScrapeGraphAiSmartScraperAction.ACTION_DEFINITION,
                        ScrapeGraphAiStartCrawlAction.ACTION_DEFINITION,
                        ScrapeGraphAiGetCrawlStatusAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ScrapeGraphAiConnection.CONNECTION_DEFINITION))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
