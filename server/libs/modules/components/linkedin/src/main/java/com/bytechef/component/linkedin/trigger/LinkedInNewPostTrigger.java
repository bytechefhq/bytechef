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

package com.bytechef.component.linkedin.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class LinkedInNewPostTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPost")
        .title("New Post")
        .description("Triggers when a new post is created in a specific organization.")
        .type(TriggerType.POLLING)
        .properties(
            string(URN)
                .label("Organization URN")
                .description("URN of the organization to monitor for new posts.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                bool("isReshareDisabledByAuthor")
                                    .description(
                                        "Indicates whether resharing of the post has been disabled by the author."),
                                integer("createdAt")
                                    .description("Time at which the resource was created in milliseconds since epoch."),
                                string("lifecycleState")
                                    .description("The state of the content."),
                                integer("lastModifiedAt")
                                    .description(
                                        "Time at which the resource was last modified in milliseconds since epoch."),
                                string("visibility")
                                    .description("Visibility restrictions on content."),
                                integer("publishedAt")
                                    .description(
                                        "The time at which the content was published represented in epoch time."),
                                string("author")
                                    .description("URN of the author of the content."),
                                string("id")
                                    .description("Unique ID for the object."),
                                object("distribution")
                                    .description("Distribution of the post both in LinkedIn and externally.")
                                    .properties(
                                        string("feedDistribution")
                                            .description("Specifies the feeds distributed to within LinkedIn."),
                                        array("thirdPartyDistributionChannels")
                                            .description(
                                                "External distribution channels that this post is distributed to.")
                                            .items(string())),
                                object("content")
                                    .description("The posted content.")
                                    .properties(
                                        object("article")
                                            .description("Details of Article content.")
                                            .properties(
                                                string("description")
                                                    .description("Custom or saved description of the article."),
                                                string("thumbnail")
                                                    .description("Custom or saved thumbnail for the article."),
                                                string("source")
                                                    .description("A URL of the article"),
                                                string("title")
                                                    .description("Custom or saved title of the article.")),
                                        object("media")
                                            .description("Details of the Media content such as Image, Video.")
                                            .properties(
                                                string("id")
                                                    .description("The URN of the media such as image or video."),
                                                string("title")
                                                    .description("The media title."),
                                                string("altText")
                                                    .description("The alternate text for the media.")),
                                        object("multiImage")
                                            .description("")
                                            .properties(
                                                array("images")
                                                    .description("The array of images in the MultiImage content.")
                                                    .items(
                                                        object()
                                                            .properties(
                                                                string("id")
                                                                    .description("The URN of the image."),
                                                                string("title")
                                                                    .description("The image title."),
                                                                string("altText")
                                                                    .description("The alternate text for the image."))),
                                                string("altText")
                                                    .description("The alternate text of this thumbnail."))),
                                string("commentary")
                                    .description("The user generated commentary for the post."),
                                object("lifecycleStateInfo")
                                    .description(
                                        "Additional information about the lifecycle state for PUBLISH_REQUESTED or PUBLISH_FAILED.")
                                    .properties(
                                        bool("isEditedByAuthor"))))))
        .poll(LinkedInNewPostTrigger::poll);

    private LinkedInNewPostTrigger() {
    }

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        List<Map<?, ?>> newPosts = new ArrayList<>();

        Map<String, ?> bodyMap = context.http(http -> http.get("/rest/posts"))
            .queryParameters(
                "q", "author",
                "author", "urn:li:organization:" + inputParameters.getRequiredString(URN),
                "sortBy", "CREATED",
                "count", 100)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Object> allElements = new ArrayList<>();

        if (bodyMap.get("elements") instanceof List<?> list) {
            allElements.addAll(list);
        }

        String nextLink = null;

        do {
            if (bodyMap.get("paging") instanceof Map<?, ?> paging && paging.get("links") instanceof List<?> links) {
                for (Object link : links) {
                    if (link instanceof Map<?, ?> map) {
                        String rel = (String) map.get("rel");

                        if (rel.equals("next")) {
                            nextLink = (String) map.get("href");

                            String finalNextLink = nextLink;

                            bodyMap = context.http(http -> http.get(finalNextLink))
                                .configuration(Http.responseType(Http.ResponseType.JSON))
                                .execute()
                                .getBody(new TypeReference<>() {});

                            if (bodyMap.get("elements") instanceof List<?> list) {
                                allElements.addAll(list);
                            }

                            break;
                        } else {
                            nextLink = null;
                        }
                    }
                }
            }
        } while (nextLink != null);

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        long epochMilli = startDate.atZone(zoneId)
            .toInstant()
            .toEpochMilli();

        for (Object element : allElements) {
            if (element instanceof Map<?, ?> map) {
                Long createdAt = (Long) map.get("createdAt");

                if (createdAt > epochMilli) {
                    newPosts.add(map);
                }
            }
        }

        return new PollOutput(newPosts, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
