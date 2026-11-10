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

import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.AUTHOR;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COMMENT_OUTPUT_PROPERTY;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COUNT;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.CREATED;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.ELEMENTS;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.ID;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.linkedin.util.LinkedInUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LinkedInNewCommentTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newComment")
        .title("New Comment")
        .description("Triggers when a new comment is added to one of the recent posts of a specific organization.")
        .help("", "https://docs.bytechef.io/reference/components/linkedin_v1#new-comment")
        .type(TriggerType.POLLING)
        .properties(
            string(URN)
                .label("Organization Number")
                .description("Number of the organization whose recent posts are monitored for new comments.")
                .required(true))
        .output(outputSchema(COMMENT_OUTPUT_PROPERTY))
        .poll(LinkedInNewCommentTrigger::poll);

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";
    protected static final int COMMENTS_PAGE_SIZE = 100;
    protected static final int RECENT_POSTS_COUNT = 10;

    private LinkedInNewCommentTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        long startEpochMilli = startDate.atZone(zoneId)
            .toInstant()
            .toEpochMilli();

        List<Map<?, ?>> newComments = new ArrayList<>();

        for (String postUrn : getRecentPostUrns(inputParameters.getRequiredString(URN), context)) {
            for (Map<?, ?> comment : getAllComments(postUrn, context)) {
                if (isCreatedAfter(comment, startEpochMilli)) {
                    newComments.add(comment);
                }
            }
        }

        return new PollOutput(newComments, Map.of(LAST_TIME_CHECKED, now), false);
    }

    private static List<Map<?, ?>> getAllComments(String postUrn, TriggerContext context) {
        List<Map<?, ?>> comments = new ArrayList<>();
        int start = 0;
        List<Map<?, ?>> page;

        do {
            page = LinkedInUtils.getComments(context, postUrn, start, COMMENTS_PAGE_SIZE);

            comments.addAll(page);

            start += COMMENTS_PAGE_SIZE;
        } while (page.size() == COMMENTS_PAGE_SIZE);

        return comments;
    }

    private static List<String> getRecentPostUrns(String organizationNumber, TriggerContext context) {
        Map<String, Object> bodyMap = context.http(http -> http.get("/rest/posts"))
            .queryParameters(
                "q", AUTHOR,
                AUTHOR, "urn:li:organization:" + organizationNumber,
                "sortBy", "CREATED",
                COUNT, RECENT_POSTS_COUNT)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<String> postUrns = new ArrayList<>();

        if (bodyMap != null && bodyMap.get(ELEMENTS) instanceof List<?> elements) {
            for (Object element : elements) {
                if (element instanceof Map<?, ?> post && post.get(ID) instanceof String postUrn) {
                    postUrns.add(postUrn);
                }
            }
        }

        return postUrns;
    }

    private static boolean isCreatedAfter(Map<?, ?> comment, long epochMilli) {
        return comment.get(CREATED) instanceof Map<?, ?> created && created.get("time") instanceof Number time &&
            time.longValue() > epochMilli;
    }
}
