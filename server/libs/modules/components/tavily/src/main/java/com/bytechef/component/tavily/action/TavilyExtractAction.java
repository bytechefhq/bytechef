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

package com.bytechef.component.tavily.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
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
public class TavilyExtractAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("extract")
        .title("Extract")
        .description("Extract web page content from one or more specified URLs.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/extract", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(array("urls").items(string().metadata(
            Map.of(
                "type", PropertyType.BODY))
            .description("A list of URLs to extract content from."))
            .placeholder("Add to Urls")
            .metadata(
                Map.of(
                    "type", PropertyType.BODY))
            .label("URLs")
            .description("A list of URLs to extract content from.")
            .required(true),
            bool("include_images").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Include Images")
                .description("Include a list of images extracted from the URLs in the response.")
                .defaultValue(false)
                .required(false),
            string("extract_depth").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Extract Depth")
                .description("The depth of the extraction process.")
                .defaultValue("basic")
                .required(false))
        .output(outputSchema(object()
            .properties(array("results").items(object().properties(string("url")
                .description("The URL from which the content was extracted.")
                .required(false),
                string("raw_content").description("The full content extracted from the page.")
                    .required(false),
                array("images").items(string().description(
                    "This is only available if include_images is set to true. A list of image URLs extracted from the page."))
                    .description(
                        "This is only available if include_images is set to true. A list of image URLs extracted from the page.")
                    .required(false))
                .description("A list of extracted content from the provided URLs."))
                .description("A list of extracted content from the provided URLs.")
                .required(false),
                array("failed_results")
                    .items(object().properties(string("url").description("The URL that failed to be processed.")
                        .required(false),
                        string("error").description("An error message describing why the URL couldn't be processed.")
                            .required(false))
                        .description("A list of URLs that could not be processed."))
                    .description("A list of URLs that could not be processed.")
                    .required(false),
                number("response_time").description("Time in seconds it took to complete the request.")
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private TavilyExtractAction() {
    }
}
