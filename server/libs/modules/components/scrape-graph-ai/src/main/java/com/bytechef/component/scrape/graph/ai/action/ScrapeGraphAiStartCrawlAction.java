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

package com.bytechef.component.scrape.graph.ai.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ScrapeGraphAiStartCrawlAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("startCrawl")
        .title("Start SmartCrawler")
        .description("Start a new web crawl request with AI extraction or markdown conversion.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/crawl", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("url").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("URL")
            .description("The starting URL for the crawl.")
            .required(true),
            string("prompt").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Prompt")
                .description("Instructions for data extraction. Required when extraction_mode is true.")
                .required(false),
            bool("extraction_mode").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Extraction Mode")
                .description("When false, enables markdown conversion mode (2 credits per page). Default is true.")
                .defaultValue(true)
                .required(false),
            bool("cache_website").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Cache Website")
                .description("Whether to cache the website content.")
                .defaultValue(false)
                .required(false),
            integer("depth").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Depth")
                .description("Maximum crawl depth.")
                .defaultValue(1)
                .required(false),
            integer("max_pages").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Max Pages")
                .description("Maximum number of pages to crawl.")
                .defaultValue(10)
                .required(false),
            bool("same_domain_only").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Same Domain Only")
                .description("Whether to crawl only the same domain.")
                .defaultValue(true)
                .required(false),
            integer("batch_size").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Batch Size")
                .description("Number of pages to process in each batch.")
                .defaultValue(1)
                .required(false),
            object("schema").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Schema")
                .description("JSON Schema object for structured output.")
                .required(false),
            object("rules")
                .properties(
                    array("exclude")
                        .items(string()
                            .description("List of URL patterns (regex) to exclude from crawling. Matches full URL."))
                        .placeholder("Add to Exclude")
                        .label("Exclude")
                        .description("List of URL patterns (regex) to exclude from crawling. Matches full URL.")
                        .required(false),
                    array("include_paths")
                        .items(string().description(
                            "List of path patterns to include (e.g., ['/products/*', '/blog/**']). Supports wildcards: * matches any characters, ** matches any path segments. If empty or not specified, all paths are included."))
                        .placeholder("Add to Include Paths")
                        .label("Include Paths")
                        .description(
                            "List of path patterns to include (e.g., ['/products/*', '/blog/**']). Supports wildcards: * matches any characters, ** matches any path segments. If empty or not specified, all paths are included.")
                        .required(false),
                    array("exclude_paths").items(string().description(
                        "List of path patterns to exclude (e.g., ['/admin/*', '/api/**']). Supports wildcards: * matches any characters, ** matches any path segments. Takes precedence over include_paths. If empty or not specified, no paths are excluded."))
                        .placeholder("Add to Exclude Paths")
                        .label("Exclude Paths")
                        .description(
                            "List of path patterns to exclude (e.g., ['/admin/*', '/api/**']). Supports wildcards: * matches any characters, ** matches any path segments. Takes precedence over include_paths. If empty or not specified, no paths are excluded.")
                        .required(false),
                    bool("same_domain").label("Same Domain")
                        .description("Restrict crawling to same domain.")
                        .defaultValue(true)
                        .required(false))
                .metadata(
                    Map.of(
                        "type", PropertyType.BODY))
                .label("Rules")
                .description("Crawl rules for filtering URLs.")
                .required(false),
            bool("sitemap").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Sitemap")
                .description("Use sitemap.xml for discovery.")
                .defaultValue(false)
                .required(false),
            bool("render_heavy_js").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Render Heavy JS")
                .description("Enable heavy JavaScript rendering.")
                .defaultValue(false)
                .required(false),
            bool("stealth").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Stealth")
                .description(
                    "Enable stealth mode to bypass bot protection using advanced anti-detection techniques. Adds +4 credits to the request cost.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(string("task_id")
                        .description(
                            "Unique identifier for the crawl task. Use this task_id to retrieve the crawl result.")
                        .required(false))
                    .metadata(
                        Map.of(
                            "responseType", ResponseType.JSON))));

    private ScrapeGraphAiStartCrawlAction() {
    }
}
