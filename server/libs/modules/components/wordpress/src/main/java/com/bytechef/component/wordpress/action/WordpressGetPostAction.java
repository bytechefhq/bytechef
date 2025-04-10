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

package com.bytechef.component.wordpress.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class WordpressGetPostAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("getPost")
        .title("Get Post")
        .description("Get a post by post ID.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/wp/v2/posts/{postId}"

            ))
        .properties(string("postId").label("Post ID")
            .description("ID of the post that will be fetched.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)))
        .output(outputSchema(object().properties(integer("id").description("Unique identifier for the post.")
            .required(false),
            string("date").description("The date the post was published, in the site's timezone.")
                .required(false),
            string("date_gmt").description("The date the post was published, in GMT.")
                .required(false),
            object("guid").properties(string("rendered").description("GUID for the post, as rendered in HTML.")
                .required(false))
                .description("The globally unique identifier for the post.")
                .required(false),
            string("modified").description("The date the post was last modified, in the site's timezone.")
                .required(false),
            string("modified_gmt").description("The date the post was last modified, in GMT.")
                .required(false),
            string("slug").description("An alphanumeric identifier for the post unique to its type.")
                .required(false),
            string("status").description("The publication status of the post.")
                .options(option("Publish", "publish"), option("Future", "future"), option("Draft", "draft"),
                    option("Pending", "pending"), option("Private", "private"))
                .required(false),
            string("type").description("Type of the object (post).")
                .required(false),
            string("link").description("URL to the post.")
                .required(false),
            object("title").properties(string("rendered").description("HTML-rendered title of the post.")
                .required(false),
                string("raw").description("Raw title of the post.")
                    .required(false))
                .description("The title for the post.")
                .required(false),
            object("content").properties(string("rendered").description("HTML-rendered content of the post.")
                .required(false),
                bool("protected").description("Whether the content is protected with a password.")
                    .required(false))
                .description("The content for the post.")
                .required(false),
            object("excerpt").properties(string("rendered").description("HTML-rendered excerpt of the post.")
                .required(false),
                bool("protected").description("Whether the excerpt is protected with a password.")
                    .required(false))
                .description("The excerpt for the post.")
                .required(false),
            integer("author").description("The ID for the author of the post.")
                .required(false),
            integer("featured_media").description("The ID of the featured media for the post.")
                .required(false),
            string("comment_status").description("Whether comments are allowed for the post.")
                .options(option("Open", "open"), option("Closed", "closed"))
                .required(false),
            string("ping_status").description("Whether pingbacks or trackbacks are allowed.")
                .options(option("Open", "open"), option("Closed", "closed"))
                .required(false),
            bool("sticky").description("Weather the post is pinned to the top of the page.")
                .required(false),
            string("template").description("The theme file used to display the post.")
                .required(false),
            string("format").description("The format of the post.")
                .required(false),
            object("meta").properties(string("footnotes").description("Footnotes of the post.")
                .required(false))
                .description("Meta fields associated with the post.")
                .required(false),
            array("categories").items(integer(null).description("Categories of the post."))
                .description("Categories of the post.")
                .required(false),
            array("tags").items(string().description("Tags of the post."))
                .description("Tags of the post.")
                .required(false),
            array("class_list").items(string().description("Class list of the post."))
                .description("Class list of the post.")
                .required(false),
            object(
                "_links")
                    .properties(
                        array("self")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                object("targetHints").properties(array("allow").items(string())
                                    .required(false))
                                    .description("Target hints of the post.")
                                    .required(false))
                                .description("Link to the current post."))
                            .description("Link to the current post.")
                            .required(false),
                        array(
                            "collection")
                                .items(object().properties(string("href").description("URL to the linked resource.")
                                    .required(false))
                                    .description("Link to the collection this post belongs to."))
                                .description("Link to the collection this post belongs to.")
                                .required(false),
                        array("about")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false))
                                .description("Link to the schema definition."))
                            .description("Link to the schema definition.")
                            .required(false),
                        array("author")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                bool("embeddable").description("Indicates whether the resource can be embedded.")
                                    .required(false))
                                .description("Link to the author of the post."))
                            .description("Link to the author of the post.")
                            .required(false),
                        array("replies")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                bool("embeddable").description("Indicates whether the resource can be embedded.")
                                    .required(false))
                                .description("Link to comments or replies for this post."))
                            .description("Link to comments or replies for this post.")
                            .required(false),
                        array("version-history")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                integer("count").description("Count of the versions.")
                                    .required(false))
                                .description("Link to the version history of this post."))
                            .description("Link to the version history of this post.")
                            .required(false),
                        array("predecessor-version")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                integer("id").description("Id of the predecessor version.")
                                    .required(false))
                                .description("Link to the predecessor version of this post."))
                            .description("Link to the predecessor version of this post.")
                            .required(false),
                        array("wp:attachment")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false))
                                .description("Link to media items attached to this post."))
                            .description("Link to media items attached to this post.")
                            .required(false),
                        array("wp:term")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                string("taxonomy").description("Taxonomy of the term.")
                                    .required(false),
                                bool("embeddable").description("Indicates whether the resource can be embedded.")
                                    .required(false))
                                .description("Terms of this post."))
                            .description("Terms of this post.")
                            .required(false),
                        array("curies").items(object().properties(string("name").description("Namespace prefix.")
                            .required(false),
                            string("href").description("Templated URI to the namespace documentation.")
                                .required(false),
                            bool("templated").description("Whether the link is templated.")
                                .required(false))
                            .description("Compact URIs to identify WordPress REST namespaces."))
                            .description("Compact URIs to identify WordPress REST namespaces.")
                            .required(false))
                    .description("Links to other related resources.")
                    .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private WordpressGetPostAction() {
    }
}
