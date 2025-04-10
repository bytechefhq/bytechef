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
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class WordpressCreatePageAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("createPage")
        .title("Create Page")
        .description("Creates a new page.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/wp/v2/pages", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("title").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("Title")
            .description("Title of the page about to be added.")
            .required(true),
            string("content").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Content")
                .description(
                    "Content of the page about to be added. Uses the WordPress Text Editor which supports HTML.")
                .required(true),
            string("status").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Status")
                .description("Status of the page about to be added.")
                .options(option("Publish", "publish"), option("Future", "future"), option("Draft", "draft"),
                    option("Pending", "pending"), option("Private", "private"))
                .required(false),
            string("slug").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Slug")
                .description("Slug of the page identifier.")
                .required(false),
            string("comment_status").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Enable Comments")
                .description("Enable comments on the page.")
                .options(option("Open", "open"), option("Closed", "closed"))
                .required(false),
            string("ping_status").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Open to Pinging")
                .description("Enable pinging on the page.")
                .options(option("Open", "open"), option("Closed", "closed"))
                .required(false))
        .output(outputSchema(object().properties(integer("id").description("Unique identifier for the page.")
            .required(false),
            string("date").description("The date the page was published, in the site's timezone.")
                .required(false),
            string("date_gmt").description("The date the page was published, in GMT.")
                .required(false),
            object("guid").properties(string("rendered").description("GUID for the page, as rendered in HTML.")
                .required(false),
                string("raw").description("GUID for the page, raw.")
                    .required(false))
                .description("The globally unique identifier for the page.")
                .required(false),
            string("modified").description("The date the page was last modified, in the site's timezone.")
                .required(false),
            string("modified_gmt").description("The date the page was last modified, in GMT.")
                .required(false),
            string("password").description("Password for accessing the page.")
                .required(false),
            string("slug").description("An alphanumeric identifier for the page unique to its type.")
                .required(false),
            string("status").description("The publication status of the page.")
                .options(option("Publish", "publish"), option("Future", "future"), option("Draft", "draft"),
                    option("Pending", "pending"), option("Private", "private"))
                .required(false),
            string("type").description("Type of the object (page).")
                .required(false),
            string("link").description("URL to the page.")
                .required(false),
            object("title").properties(string("rendered").description("HTML-rendered title of the page.")
                .required(false),
                string("raw").description("Raw title of the page.")
                    .required(false))
                .description("The title for the page.")
                .required(false),
            object("content").properties(string("rendered").description("HTML-rendered content of the page.")
                .required(false),
                string("raw").description("Raw content of the page.")
                    .required(false),
                bool("protected").description("Whether the content is protected with a password.")
                    .required(false),
                integer("block_version").description("Block version of the page content.")
                    .required(false))
                .description("The content for the page.")
                .required(false),
            object("excerpt").properties(string("raw").description("Raw excerpt of the page.")
                .required(false),
                string("rendered").description("HTML-rendered excerpt of the page.")
                    .required(false),
                bool("protected").description("Whether the excerpt is protected with a password.")
                    .required(false))
                .description("The excerpt for the page.")
                .required(false),
            integer("author").description("The ID for the author of the page.")
                .required(false),
            integer("featured_media").description("The ID of the featured media for the page.")
                .required(false),
            integer("parent").description("The ID of the parent page, if any.")
                .required(false),
            integer("menu_order").description("The order of the page in menus.")
                .required(false),
            string("comment_status").description("Whether comments are allowed for the page.")
                .options(option("Open", "open"), option("Closed", "closed"))
                .required(false),
            string("ping_status").description("Whether pingbacks or trackbacks are allowed.")
                .options(option("Open", "open"), option("Closed", "closed"))
                .required(false),
            string("template").description("The theme file used to display the page.")
                .required(false),
            object("meta").properties(string("footnotes").description("Footnotes of the page.")
                .required(false))
                .description("Meta fields associated with the page.")
                .required(false),
            string("permalink_template").description("Permalink template of the page.")
                .required(false),
            string("generated_slug").description("Generated slug of the page.")
                .required(false),
            array("class_list").items(string().description("Class list of the page."))
                .description("Class list of the page.")
                .required(false),
            object(
                "_links")
                    .properties(
                        array("self")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                object("targetHints").properties(array("allow").items(string())
                                    .required(false))
                                    .description("Target hints of the page.")
                                    .required(false))
                                .description("Link to the current page."))
                            .description("Link to the current page.")
                            .required(false),
                        array(
                            "collection")
                                .items(object().properties(string("href").description("URL to the linked resource.")
                                    .required(false))
                                    .description("Link to the collection this page belongs to."))
                                .description("Link to the collection this page belongs to.")
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
                                .description("Link to the author of the page."))
                            .description("Link to the author of the page.")
                            .required(false),
                        array("replies")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                bool("embeddable").description("Indicates whether the resource can be embedded.")
                                    .required(false))
                                .description("Link to comments or replies for this page."))
                            .description("Link to comments or replies for this page.")
                            .required(false),
                        array("version-history")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false),
                                integer("count").description("Count of the versions.")
                                    .required(false))
                                .description("Link to the version history of this page."))
                            .description("Link to the version history of this page.")
                            .required(false),
                        array("wp:attachment")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false))
                                .description("Link to media items attached to this page."))
                            .description("Link to media items attached to this page.")
                            .required(false),
                        array("wp:action-publish")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false))
                                .description("Action link to publish this page."))
                            .description("Action link to publish this page.")
                            .required(false),
                        array("wp:action-unfiltered-html")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false))
                                .description("Action link for unfiltered HTML editing."))
                            .description("Action link for unfiltered HTML editing.")
                            .required(false),
                        array("wp:action-assign-author")
                            .items(object().properties(string("href").description("URL to the linked resource.")
                                .required(false))
                                .description("Action link to assign a new author to this page."))
                            .description("Action link to assign a new author to this page.")
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

    private WordpressCreatePageAction() {
    }
}
