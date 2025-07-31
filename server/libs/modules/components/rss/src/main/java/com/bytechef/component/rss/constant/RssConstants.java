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

package com.bytechef.component.rss.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Nikolina Spehar
 */
public class RssConstants {

    public static final String API_KEY = "apiKey";
    public static final String API_SECRET = "apiSecret";

    public static final ModifiableObjectProperty ITEM_OBJECT = object()
        .description("Feed item object")
        .properties(
            string("url")
                .description("URL of the item on the feed."),
            string("title")
                .description("Title of the item."),
            string("description_text")
                .description("Description of the item."),
            string("thumbnail")
                .description("Thumbnail of the item."),
            string("date_published")
                .description("Date when the item was published."),
            array("authors")
                .description("Authors of the item.")
                .items(
                    string("author")));
}
