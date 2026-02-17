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

package com.bytechef.component.firecrawl.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ALLOW_EXTERNAL_LINKS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ALLOW_SUBDOMAINS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.BLOCK_ADS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.CRAWL_ENTIRE_DOMAIN;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.DELAY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.EXCLUDE_PATHS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.EXCLUDE_TAGS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.FORMATS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.HEADERS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.IGNORE_QUERY_PARAMETERS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INCLUDE_PATHS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INCLUDE_TAGS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LIMIT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LOCATION;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.MAX_AGE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.MAX_CONCURRENCY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.MAX_DISCOVERY_DEPTH;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.MOBILE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ONLY_MAIN_CONTENT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PARSERS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROMPT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROXY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.REGEX_ON_FULL_URL;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.REMOVE_BASE64_IMAGES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SCRAPE_OPTIONS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SITEMAP;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SKIP_TLS_VERIFICATION;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.STORE_IN_CACHE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TIMEOUT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.URL;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.WAIT_FOR;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.WEBHOOK;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ZERO_DATA_RETENTION;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Krišković
 */
public class FirecrawlCrawlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("crawl")
        .title("Crawl")
        .description("Crawl multiple URLs starting from a base URL and extract content.")
        .properties(
            string(URL)
                .label("URL")
                .description("The base URL to start crawling from.")
                .required(true),
            array(FORMATS)
                .label("Formats")
                .description("Output formats to include in the response for each crawled page.")
                .items(string())
                .options(List.of(
                    option("Markdown", "markdown"),
                    option("HTML", "html"),
                    option("Raw HTML", "rawHtml"),
                    option("Links", "links")))
                .required(false),
            string(PROMPT)
                .label("Prompt")
                .description(
                    "A natural language prompt to generate crawler options. Explicitly set parameters will " +
                        "override the generated equivalents.")
                .required(false),
            array(EXCLUDE_PATHS)
                .label("Exclude Paths")
                .description("URL pathname regex patterns that exclude matching URLs from the crawl.")
                .items(string())
                .required(false),
            array(INCLUDE_PATHS)
                .label("Include Paths")
                .description(
                    "URL pathname regex patterns that include matching URLs in the crawl. Only paths matching " +
                        "the specified patterns will be included.")
                .items(string())
                .required(false),
            integer(MAX_DISCOVERY_DEPTH)
                .label("Max Discovery Depth")
                .description(
                    "Maximum depth to crawl based on discovery order. The root site and sitemapped pages have " +
                        "a discovery depth of 0.")
                .required(false),
            string(SITEMAP)
                .label("Sitemap")
                .description(
                    "Sitemap mode: 'include' uses sitemap and other methods (default), 'skip' ignores the " +
                        "sitemap, 'only' crawls only sitemap URLs.")
                .options(
                    option("Include", "include"),
                    option("Skip", "skip"),
                    option("Only", "only"))
                .required(false),
            integer(LIMIT)
                .label("Limit")
                .description("Maximum number of pages to crawl. Default limit is 10000.")
                .required(false),
            object(SCRAPE_OPTIONS)
                .label("Scrape Options")
                .description("Options for scraping each page during the crawl.")
                .properties(
                    bool(ONLY_MAIN_CONTENT)
                        .label("Only Main Content")
                        .description("Only return the main content excluding headers, navs, footers, etc.")
                        .required(false),
                    array(INCLUDE_TAGS)
                        .label("Include Tags")
                        .description("HTML tags to include in the output.")
                        .items(string())
                        .required(false),
                    array(EXCLUDE_TAGS)
                        .label("Exclude Tags")
                        .description("HTML tags to exclude from the output.")
                        .items(string())
                        .required(false),
                    integer(MAX_AGE)
                        .label("Max Age")
                        .description(
                            "Returns a cached version if younger than this age in milliseconds. Default is 2 days " +
                                "(172800000ms).")
                        .required(false),
                    object(HEADERS)
                        .label("Headers")
                        .description("Custom headers to send with the request (e.g., cookies, user-agent).")
                        .required(false),
                    integer(WAIT_FOR)
                        .label("Wait For")
                        .description("Delay in milliseconds before fetching content.")
                        .required(false),
                    bool(MOBILE)
                        .label("Mobile")
                        .description("Emulate scraping from a mobile device.")
                        .required(false),
                    bool(SKIP_TLS_VERIFICATION)
                        .label("Skip TLS Verification")
                        .description("Skip TLS certificate verification when making requests.")
                        .required(false),
                    integer(TIMEOUT)
                        .label("Timeout")
                        .description(
                            "Timeout in milliseconds for the request. Maximum is 300000 (5 minutes).")
                        .maxValue(300000)
                        .required(false),
                    bool(REMOVE_BASE64_IMAGES)
                        .label("Remove Base64 Images")
                        .description("Removes all base64 images from output.")
                        .required(false),
                    bool(BLOCK_ADS)
                        .label("Block Ads")
                        .description("Enables ad-blocking and cookie popup blocking.")
                        .required(false),
                    string(PROXY)
                        .label("Proxy")
                        .description(
                            "Proxy type: 'basic' (fast, basic anti-bot), 'enhanced' (slower, advanced anti-bot, " +
                                "costs up to 5 credits), 'auto' (retries with enhanced if basic fails).")
                        .options(
                            option("Auto", "auto"),
                            option("Basic", "basic"),
                            option("Enhanced", "enhanced"))
                        .required(false),
                    object(LOCATION)
                        .label("Location")
                        .description(
                            "Location settings for the request. Uses appropriate proxy and emulates " +
                                "language/timezone.")
                        .properties(
                            string("country")
                                .label("Country")
                                .description("ISO 3166-1 alpha-2 country code (e.g., 'US', 'AU', 'DE', 'JP')."),
                            array("languages")
                                .label("Languages")
                                .description("Preferred languages in order of priority (e.g., ['en-US', 'en']).")
                                .items(string()))
                        .required(false),
                    array(PARSERS)
                        .label("Parsers")
                        .description(
                            "Controls how files are processed. When 'pdf' is included (default), PDF content is " +
                                "extracted and converted to markdown (1 credit per page).")
                        .items(
                            object()
                                .properties(
                                    string("type")
                                        .options(option("PDF", "pdf"))
                                        .required(true),
                                    integer("maxPages")
                                        .label("Max Pages")
                                        .description("Maximum number of PDF pages to parse (1-10000).")
                                        .minValue(1)
                                        .maxValue(10000)))
                        .required(false),
                    bool(STORE_IN_CACHE)
                        .label("Store in Cache")
                        .description(
                            "If true, pages will be stored in Firecrawl index and cache. Set to false for data " +
                                "protection concerns.")
                        .required(false))
                .advancedOption(true)
                .required(false),
            bool(IGNORE_QUERY_PARAMETERS)
                .label("Ignore Query Parameters")
                .description("Do not re-scrape the same path with different (or none) query parameters.")
                .advancedOption(true)
                .required(false),
            bool(REGEX_ON_FULL_URL)
                .label("Regex on Full URL")
                .description(
                    "When true, includePaths and excludePaths patterns are matched against the full URL " +
                        "including query parameters.")
                .advancedOption(true)
                .required(false),
            bool(CRAWL_ENTIRE_DOMAIN)
                .label("Crawl Entire Domain")
                .description(
                    "Allows the crawler to follow internal links to sibling or parent URLs, not just child paths.")
                .advancedOption(true)
                .required(false),
            bool(ALLOW_EXTERNAL_LINKS)
                .label("Allow External Links")
                .description("Allows the crawler to follow links to external websites.")
                .advancedOption(true)
                .required(false),
            bool(ALLOW_SUBDOMAINS)
                .label("Allow Subdomains")
                .description("Allows the crawler to follow links to subdomains of the main domain.")
                .advancedOption(true)
                .required(false),
            integer(DELAY)
                .label("Delay")
                .description("Delay in seconds between scrapes. Helps respect website rate limits.")
                .advancedOption(true)
                .required(false),
            integer(MAX_CONCURRENCY)
                .label("Max Concurrency")
                .description(
                    "Maximum number of concurrent scrapes. If not specified, adheres to your team's concurrency " +
                        "limit.")
                .advancedOption(true)
                .required(false),
            object(WEBHOOK)
                .label("Webhook")
                .description("Webhook configuration to receive crawl status updates.")
                .properties(
                    string("url")
                        .label("URL")
                        .description(
                            "The URL to send webhook events to. Triggers for crawl.started, crawl.page, " +
                                "crawl.completed, and crawl.failed events.")
                        .required(true),
                    object("headers")
                        .label("Headers")
                        .description("Headers to send to the webhook URL.")
                        .required(false),
                    object("metadata")
                        .label("Metadata")
                        .description("Custom metadata included in all webhook payloads for this crawl.")
                        .required(false),
                    array("events")
                        .label("Events")
                        .description("Types of events to send to the webhook. Defaults to all events.")
                        .items(string())
                        .options(List.of(
                            option("Completed", "completed"),
                            option("Page", "page"),
                            option("Failed", "failed"),
                            option("Started", "started")))
                        .required(false))
                .advancedOption(true)
                .required(false),
            bool(ZERO_DATA_RETENTION)
                .label("Zero Data Retention")
                .description(
                    "Enable zero data retention for this crawl. Contact help@firecrawl.dev to enable this feature.")
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success"),
                        string("id"),
                        string("url"))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#crawl")
        .perform(FirecrawlCrawlAction::perform);

    private FirecrawlCrawlAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> scrapeOptionsMap = new HashMap<>();

        List<?> formatsList = inputParameters.getList(FORMATS);

        if (formatsList != null) {
            scrapeOptionsMap.put(FORMATS, formatsList);
        }

        Object scrapeOptionsObj = inputParameters.get(SCRAPE_OPTIONS);

        if (scrapeOptionsObj instanceof Map<?, ?> scrapeOptions) {
            for (Map.Entry<?, ?> entry : scrapeOptions.entrySet()) {
                scrapeOptionsMap.put(entry.getKey()
                    .toString(), entry.getValue());
            }
        }

        return context
            .http(http -> http.post("/crawl"))
            .body(
                Http.Body.of(
                    URL, inputParameters.getRequiredString(URL),
                    PROMPT, inputParameters.getString(PROMPT),
                    EXCLUDE_PATHS, inputParameters.getList(EXCLUDE_PATHS),
                    INCLUDE_PATHS, inputParameters.getList(INCLUDE_PATHS),
                    MAX_DISCOVERY_DEPTH, inputParameters.getInteger(MAX_DISCOVERY_DEPTH),
                    SITEMAP, inputParameters.getString(SITEMAP),
                    IGNORE_QUERY_PARAMETERS, inputParameters.getBoolean(IGNORE_QUERY_PARAMETERS),
                    REGEX_ON_FULL_URL, inputParameters.getBoolean(REGEX_ON_FULL_URL),
                    LIMIT, inputParameters.getInteger(LIMIT),
                    CRAWL_ENTIRE_DOMAIN, inputParameters.getBoolean(CRAWL_ENTIRE_DOMAIN),
                    ALLOW_EXTERNAL_LINKS, inputParameters.getBoolean(ALLOW_EXTERNAL_LINKS),
                    ALLOW_SUBDOMAINS, inputParameters.getBoolean(ALLOW_SUBDOMAINS),
                    DELAY, inputParameters.getInteger(DELAY),
                    MAX_CONCURRENCY, inputParameters.getInteger(MAX_CONCURRENCY),
                    WEBHOOK, inputParameters.get(WEBHOOK),
                    SCRAPE_OPTIONS, scrapeOptionsMap,
                    ZERO_DATA_RETENTION, inputParameters.getBoolean(ZERO_DATA_RETENTION)))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
