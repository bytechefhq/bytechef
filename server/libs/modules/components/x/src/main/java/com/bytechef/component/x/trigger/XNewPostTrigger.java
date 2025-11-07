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

package com.bytechef.component.x.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.x.constant.XConstants.DATA;
import static com.bytechef.component.x.constant.XConstants.ID;
import static com.bytechef.component.x.constant.XConstants.TEXT;
import static com.bytechef.component.x.constant.XConstants.USERNAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.x.util.XUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class XNewPostTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPost")
        .title("New Post")
        .description("Triggers when a new post is created by a specific user.")
        .type(TriggerType.POLLING)
        .properties(
            string(USERNAME)
                .label("Username")
                .description("The username of the user to monitor for new posts.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("ID of the Tweet."),
                        string(TEXT)
                            .description("The content of the Tweet."),
                        array("edit_history_tweet_ids")
                            .description("A list of Tweet Ids in this Tweet chain.")
                            .items(string()))))
        .poll(XNewPostTrigger::poll);

    private XNewPostTrigger() {
    }

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String username = inputParameters.getRequiredString(USERNAME);
        String userId = XUtils.getUserIdByUsername(context, username);

        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        LocalDateTime start = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        Map<String, ?> bodyMap = context.http(http -> http.get("/users/" + userId + "/tweets"))
            .queryParameters(
                "start_time", formatLocalDateTime(start),
                "end_time", formatLocalDateTime(now),
                "max_results", 100)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Object> posts = extractPosts(bodyMap);

        return new PollOutput(posts, Map.of(LAST_TIME_CHECKED, now), false);
    }

    private static String formatLocalDateTime(LocalDateTime localDateTime) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

        return localDateTime.format(dateTimeFormatter);
    }

    private static List<Object> extractPosts(Map<String, ?> bodyMap) {
        List<Object> allPosts = new ArrayList<>();

        Object data = bodyMap.get(DATA);

        if (data instanceof List<?> list) {
            allPosts.addAll(list);
        }

        return allPosts;
    }
}
