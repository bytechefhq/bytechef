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

package com.bytechef.component.wordpress.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.wordpress.constant.WordpressConstants.LAST_TIME_CHECKED;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class WordPressNewPostTrigger {
    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPost")
        .title("New Post")
        .description("Triggers when a new post is added.")
        .type(TriggerType.POLLING)
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("id")
                            .description("Unique identifier for the post."),
                        string("date")
                            .description("The date the post was published, in the site's timezone."),
                        string("date_gmt")
                            .description("The date the post was published, in GMT."),
                        object("guid")
                            .description("The globally unique identifier for the post.")
                            .properties(
                                string("rendered")
                                    .description("GUID for the post, as rendered in HTML.")),
                        string("modified")
                            .description(
                                "The date the post was last modified, in the site's timezone."),
                        string("modified_gmt")
                            .description("The date the post was last modified, in GMT."),
                        string("slug")
                            .description("An alphanumeric identifier for the post unique to its type."),
                        string("status")
                            .description("The publication status of the post."),
                        string("type")
                            .description("Type of the object (post)."),
                        string("link")
                            .description("URL to the post."),
                        object("title")
                            .description("The title for the post.")
                            .properties(
                                string("rendered")
                                    .description("HTML-rendered title of the post."),
                                string("raw")
                                    .description("Raw title of the post.")),
                        object("content")
                            .description("The content for the post.")
                            .properties(
                                string("rendered")
                                    .description("HTML-rendered content of the post."),
                                bool("protected")
                                    .description("Whether the content is protected with a password.")),
                        object("excerpt")
                            .description("The excerpt for the post.")
                            .properties(
                                string("rendered")
                                    .description("HTML-rendered excerpt of the post."),
                                bool("protected")
                                    .description("Whether the excerpt is protected with a password.")),
                        integer("author")
                            .description("The ID for the author of the post."),
                        integer("featured_media")
                            .description("The ID of the featured media for the post."),
                        string("comment_status")
                            .description("Whether comments are allowed for the post."),
                        string("ping_status")
                            .description("Whether pingbacks or trackbacks are allowed."),
                        bool("sticky")
                            .description("Weather the post is pinned to the top of the page."),
                        string("template")
                            .description("The theme file used to display the post."),
                        string("format")
                            .description("The format of the post."),
                        object("meta")
                            .description("Meta fields associated with the post.")
                            .properties(
                                string("footnotes")
                                    .description("Footnotes of the post.")),
                        array("categories")
                            .description("Categories of the post.")
                            .items(integer()),
                        array("tags")
                            .description("Tags of the post.")
                            .items(string()),
                        array("class_list")
                            .description("Class list of the post.")
                            .items(string()),
                        object("_links")
                            .description("Links to other related resources.")
                            .properties(
                                array("self")
                                    .description("Link to the current post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."),
                                                object("targetHints")
                                                    .description("Target hints of the post.")
                                                    .properties(
                                                        array("allow")
                                                            .items(string())))),
                                array("collection")
                                    .description("Link to the collection this post belongs to.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."))),
                                array("about")
                                    .description("Link to the schema definition.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."))),
                                array("author")
                                    .description("Link to the author of the post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."),
                                                bool("embeddable")
                                                    .description(
                                                        "Indicates whether the resource can be embedded."))),
                                array("replies")
                                    .description("Link to comments or replies for this post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."),
                                                bool("embeddable")
                                                    .description(
                                                        "Indicates whether the resource can be embedded."))),
                                array("version-history")
                                    .description("Link to the version history of this post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."),
                                                integer("count")
                                                    .description("Count of the versions."))),
                                array("predecessor-version")
                                    .description("Link to the predecessor version of this post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."),
                                                integer("id")
                                                    .description("Id of the predecessor version."))),
                                array("wp:attachment")
                                    .description("Link to media items attached to this post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."))),
                                array("wp:term")
                                    .description("Terms of this post.")
                                    .items(
                                        object()
                                            .properties(
                                                string("href")
                                                    .description("URL to the linked resource."),
                                                string("taxonomy")
                                                    .description("Taxonomy of the term."),
                                                bool("embeddable")
                                                    .description(
                                                        "Indicates whether the resource can be embedded."))),
                                array("curies")
                                    .description("Compact URIs to identify WordPress REST namespaces.")
                                    .items(
                                        object()
                                            .properties(
                                                string("name")
                                                    .description("Namespace prefix."),
                                                string("href")
                                                    .description(
                                                        "Templated URI to the namespace documentation."),
                                                bool("templated")
                                                    .description(
                                                        "Whether the link is templated.")))))))
        .poll(WordPressNewPostTrigger::poll);

    private WordPressNewPostTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, now.minusHours(3));

        List<Map<String, Object>> newPosts = triggerContext.http(http -> http.get("/wp/v2/posts"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .queryParameter("after", startDate.format(DateTimeFormatter.ISO_DATE_TIME))
            .execute()
            .getBody(new TypeReference<>() {});

        return new PollOutput(newPosts, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
