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

package com.bytechef.component.rss.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
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
public class RssCreateFeedAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createFeed")
        .title("Create Feed")
        .description("Creates feed from website by using website URL.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/feeds", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("url").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Url")
            .description("A valid Website URL is required (example: https://bbc.com).")
            .required(true))
        .output(outputSchema(object().properties(string("id").description("Unique ID of the RSS feed.")
            .required(false),
            string("title").description("Title of the RSS feed.")
                .required(false),
            string("rss_feed_url").description("Direct URL to the RSS XML feed.")
                .required(false),
            string("source_url").description("Original source URL for the content.")
                .required(false),
            string("description").description("Description of the RSS feed.")
                .required(false),
            array("items").items(object().properties(string("url").description("URL of the individual feed item.")
                .required(false),
                string("title").description("Title of the feed item.")
                    .required(false),
                string("description_text").description("Plain-text summary of the item.")
                    .required(false),
                string("description_html").description("HTML content of the feed item.")
                    .required(false),
                string("thumbnail").description("Thumbnail image for the item.")
                    .required(false),
                string("date_published").description("Date the item was published.")
                    .required(false),
                array("authors").items(string().description("List of authors for the item."))
                    .description("List of authors for the item.")
                    .required(false))
                .description("List of items in the RSS feed."))
                .description("List of items in the RSS feed.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private RssCreateFeedAction() {
    }
}
