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

package com.bytechef.component.devto.property;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.List;

/**
 * Provides properties definition built from OpenAPI schema.
 *
 * @generated
 */
public class DevtoArticleResponseProperties {
    public static final List<ComponentDsl.ModifiableValueProperty<?, ?>> PROPERTIES = List.of(
        string("type_of").label("Type Of")
            .required(false),
        integer("id").label("Id")
            .description("The unique identifier of the article.")
            .required(false),
        string("title").label("Title")
            .description("The title of the article.")
            .required(false),
        string("description").label("Description")
            .description("The description of the article.")
            .required(false),
        string("slug").label("Slug")
            .required(false),
        string("path").label("Path")
            .description("The path of the article.")
            .required(false),
        string("url").label("Url")
            .description("The URL of the article.")
            .required(false),
        integer("comments_count").label("Comments Count")
            .description("The number of comments on the article.")
            .required(false),
        integer("public_reactions_count").label("Public Reactions Count")
            .description("The number of public reactions on the article.")
            .required(false),
        integer("positive_reactions_count").label("Positive Reactions Count")
            .description("The number of positive reactions on the article.")
            .required(false),
        dateTime("created_at").label("Created At")
            .description("The date and time when the article was created.")
            .required(false),
        dateTime("edited_at").label("Edited At")
            .description("The date and time when the article was last edited.")
            .required(false),
        dateTime("crossposted_at").label("Crossposted At")
            .description("The date and time when the article was crossposted.")
            .required(false),
        dateTime("published_at").label("Published At")
            .description("The date and time when the article was published.")
            .required(false),
        dateTime("last_comment_at").label("Last Comment At")
            .description("The date and time of the last comment on the article.")
            .required(false),
        integer("reading_time_minutes").label("Reading Time Minutes")
            .description("The estimated reading time of the article in minutes.")
            .required(false),
        string("tag_list").label("Tag List")
            .description("The tags of the article.")
            .required(false),
        array("tags").items(string().description("The tags of the article."))
            .placeholder("Add to Tags")
            .label("Tags")
            .description("The tags of the article.")
            .required(false),
        string("body_html").label("Body Html")
            .description("The body of the article in HTML format.")
            .required(false),
        string("body_markdown").label("Body Markdown")
            .description("The body of the article in markdown format.")
            .required(false),
        object("user").properties(string("name").label("Name")
            .description("The name of the author of the article.")
            .required(false),
            string("username").label("Username")
                .description("The username of the author of the article.")
                .required(false),
            string("twitter_username").label("Twitter Username")
                .description("The Twitter username of the author of the article.")
                .required(false),
            string("github_username").label("Github Username")
                .description("The GitHub username of the author of the article.")
                .required(false),
            integer("user_id").label("User Id")
                .description("The unique identifier of the author of the article.")
                .required(false))
            .label("User")
            .required(false));

    private DevtoArticleResponseProperties() {
    }
}
