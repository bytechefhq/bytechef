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
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.BLOCK_ADS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.EXCLUDE_TAGS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.FORMATS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.FORMATS_PROMPT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.FORMATS_SCHEMA;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.HEADERS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.INCLUDE_TAGS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.LOCATION;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.MAX_AGE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.MOBILE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ONLY_MAIN_CONTENT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PARSERS;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.PROXY;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.REMOVE_BASE64_IMAGES;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.SKIP_TLS_VERIFICATION;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.STORE_IN_CACHE;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.TIMEOUT;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.URL;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.WAIT_FOR;
import static com.bytechef.component.firecrawl.constant.FirecrawlConstants.ZERO_DATA_RETENTION;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Krišković
 */
public class FirecrawlScrapeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("scrape")
        .title("Scrape URL")
        .description("Scrape a single URL and extract content in various formats.")
        .properties(
            string(URL)
                .label("URL")
                .description("The URL to scrape.")
                .required(true),
            array(FORMATS)
                .label("Formats")
                .description("Output formats to include in the response (e.g., markdown, html, json).")
                .items(string())
                .options(List.of(
                    option("Markdown", "markdown"),
                    option("Summary", "summary"),
                    option("HTML", "html"),
                    option("Raw HTML", "rawHtml"),
                    option("Images", "images"),
                    option("Links", "links"),
                    option("JSON", "json"),
                    option("Branding", "branding")))
                .required(false),
            object(FORMATS_SCHEMA)
                .label("JSON Schema")
                .description("The schema to use for the JSON output. Must conform to JSON Schema.")
                .displayCondition("contains(formats, 'json')")
                .required(false),
            object(FORMATS_PROMPT)
                .label("JSON Prompt")
                .description("The prompt to use for the JSON output")
                .displayCondition("contains(formats, 'json')")
                .required(false),
            bool(ONLY_MAIN_CONTENT)
                .label("Only Main Content")
                .description("Only return the main content excluding headers, navs, footers, etc.")
                .advancedOption(true)
                .required(false),
            array(INCLUDE_TAGS)
                .label("Include Tags")
                .description("HTML tags to include in the output.")
                .items(string())
                .advancedOption(true)
                .required(false),
            array(EXCLUDE_TAGS)
                .label("Exclude Tags")
                .description("HTML tags to exclude from the output.")
                .items(string())
                .advancedOption(true)
                .required(false),
            integer(MAX_AGE)
                .label("Max Age")
                .description(
                    "Returns a cached version if younger than this age in milliseconds. Speeds up scrapes by up " +
                        "to 500%. Default is 2 days (172800000ms).")
                .advancedOption(true)
                .required(false),
            object(HEADERS)
                .label("Headers")
                .description("Custom headers to send with the request (e.g., cookies, user-agent).")
                .advancedOption(true)
                .required(false),
            integer(WAIT_FOR)
                .label("Wait For")
                .description(
                    "Delay in milliseconds before fetching content, allowing the page to load. This is in addition " +
                        "to Firecrawl's smart wait feature.")
                .advancedOption(true)
                .required(false),
            bool(MOBILE)
                .label("Mobile")
                .description(
                    "Emulate scraping from a mobile device. Useful for responsive pages and mobile screenshots.")
                .advancedOption(true)
                .required(false),
            bool(SKIP_TLS_VERIFICATION)
                .label("Skip TLS Verification")
                .description("Skip TLS certificate verification when making requests.")
                .advancedOption(true)
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description(
                    "Timeout in milliseconds for the request. Default is 30000 (30 seconds). Maximum is " +
                        "300000 (5 minutes).")
                .maxValue(300000)
                .advancedOption(true)
                .required(false),
            bool(REMOVE_BASE64_IMAGES)
                .label("Remove Base64 Images")
                .description(
                    "Removes all base64 images from output. Image alt text remains but URL is replaced " +
                        "with placeholder.")
                .advancedOption(true)
                .required(false),
            bool(BLOCK_ADS)
                .label("Block Ads")
                .description("Enables ad-blocking and cookie popup blocking.")
                .advancedOption(true)
                .required(false),
            string(PROXY)
                .label("Proxy")
                .description(
                    "Proxy type: 'basic' (fast, basic anti-bot), 'enhanced' (slower, advanced anti-bot, costs up " +
                        "to 5 credits), 'auto' (retries with enhanced if basic fails).")
                .options(
                    option("Auto", "auto"),
                    option("Basic", "basic"),
                    option("Enhanced", "enhanced"))
                .advancedOption(true)
                .required(false),
            object(LOCATION)
                .label("Location")
                .description(
                    "Location settings for the request. Uses appropriate proxy and emulates language/timezone.")
                .properties(
                    string("country")
                        .label("Country")
                        .description("ISO 3166-1 alpha-2 country code (e.g., 'US', 'AU', 'DE', 'JP')."),
                    array("languages")
                        .label("Languages")
                        .description("Preferred languages in order of priority (e.g., ['en-US', 'en']).")
                        .items(string()))
                .advancedOption(true)
                .required(false),
            array(PARSERS)
                .label("Parsers")
                .description(
                    "Controls how files are processed. When 'pdf' is included (default), PDF content is extracted " +
                        "and converted to markdown (1 credit per page). Empty array returns PDF in base64 " +
                        "(1 credit flat).")
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
                .advancedOption(true)
                .required(false),
            bool(STORE_IN_CACHE)
                .label("Store in Cache")
                .description(
                    "If true, page will be stored in Firecrawl index and cache. Set to false for data " +
                        "protection concerns.")
                .advancedOption(true)
                .required(false),
            bool(ZERO_DATA_RETENTION)
                .label("Zero Data Retention")
                .description(
                    "Enable zero data retention for this scrape. Contact help@firecrawl.dev to enable this feature.")
                .advancedOption(true)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success"),
                        object("data")
                            .properties(
                                string("markdown"),
                                string("summary"),
                                string("html"),
                                string("rawHtml"),
                                string("screenshot"),
                                array("links")
                                    .items(string()),
                                object("metadata")
                                    .properties(
                                        string("title"),
                                        string("description"),
                                        string("language"),
                                        string("sourceURL"),
                                        string("keywords"),
                                        integer("statusCode"),
                                        string("error")),
                                string("warning")))))
        .help("", "https://docs.bytechef.io/reference/components/firecrawl_v1#scrape")
        .perform(FirecrawlScrapeAction::perform);

    private FirecrawlScrapeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<?> formatsList = inputParameters.getList(FORMATS);
        List<Object> formatObjectList = new ArrayList<>();

        for (Object format : formatsList) {
            Map<String, Object> jsonSchemaMap = new HashMap<>();
            jsonSchemaMap.put("type", format.toString());

            if (formatsList.contains("json")) {
                jsonSchemaMap.put("schema", inputParameters.get(FORMATS_SCHEMA));
                jsonSchemaMap.put("prompt", inputParameters.get(FORMATS_PROMPT));
            }

            formatObjectList.add(jsonSchemaMap);
        }

        return context
            .http(http -> http.post("/scrape"))
            .body(
                Http.Body.of(
                    URL, inputParameters.getRequiredString(URL),
                    FORMATS, formatObjectList,
                    ONLY_MAIN_CONTENT, inputParameters.getBoolean(ONLY_MAIN_CONTENT),
                    INCLUDE_TAGS, inputParameters.getList(INCLUDE_TAGS),
                    EXCLUDE_TAGS, inputParameters.getList(EXCLUDE_TAGS),
                    MAX_AGE, inputParameters.getInteger(MAX_AGE),
                    HEADERS, inputParameters.get(HEADERS),
                    WAIT_FOR, inputParameters.getInteger(WAIT_FOR),
                    MOBILE, inputParameters.getBoolean(MOBILE),
                    SKIP_TLS_VERIFICATION, inputParameters.getBoolean(SKIP_TLS_VERIFICATION),
                    TIMEOUT, inputParameters.getInteger(TIMEOUT),
                    REMOVE_BASE64_IMAGES, inputParameters.getBoolean(REMOVE_BASE64_IMAGES),
                    BLOCK_ADS, inputParameters.getBoolean(BLOCK_ADS),
                    PROXY, inputParameters.getString(PROXY),
                    LOCATION, inputParameters.get(LOCATION),
                    PARSERS, inputParameters.getList(PARSERS),
                    STORE_IN_CACHE, inputParameters.getBoolean(STORE_IN_CACHE),
                    ZERO_DATA_RETENTION, inputParameters.getBoolean(ZERO_DATA_RETENTION)))
            .configuration(Http.responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
