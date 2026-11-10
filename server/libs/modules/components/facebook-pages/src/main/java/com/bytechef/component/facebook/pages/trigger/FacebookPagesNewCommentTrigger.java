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

package com.bytechef.component.facebook.pages.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.COMMENT_FIELDS;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.CREATED_TIME;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.FROM;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.MESSAGE;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.NAME;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID;
import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.facebook.pages.util.FacebookPagesUtils;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class FacebookPagesNewCommentTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";
    protected static final String POST_ID = "post_id";

    private static final DateTimeFormatter CREATED_TIME_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ssZ");
    private static final int RECENT_POSTS_LIMIT = 10;

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newComment")
        .title("New Comment")
        .description("Triggers when a new comment is added to one of the recent posts of a Facebook Page.")
        .help("", "https://docs.bytechef.io/reference/components/facebook-pages_v1#new-comment")
        .type(TriggerType.POLLING)
        .properties(PAGE_ID_PROPERTY)
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string(ID)
                                    .description("The ID of the comment."),
                                string(MESSAGE)
                                    .description("The text of the comment."),
                                string(CREATED_TIME)
                                    .description("The time the comment was created."),
                                object(FROM)
                                    .description("The person or Page that made the comment.")
                                    .properties(
                                        string(ID)
                                            .description("The ID of the commenter."),
                                        string(NAME)
                                            .description("The name of the commenter.")),
                                string(POST_ID)
                                    .description("The ID of the post that was commented on.")))))
        .poll(FacebookPagesNewCommentTrigger::poll);

    private FacebookPagesNewCommentTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime lastTimeChecked = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        String pageId = inputParameters.getRequiredString(PAGE_ID);

        String pageAccessToken = FacebookPagesUtils.getPageAccessToken(context, pageId);

        Map<String, Object> postsBody = FacebookPagesUtils.executePageRequest(
            context.http(http -> http.get("/" + pageId + "/posts"))
                .queryParameters("fields", ID, "limit", RECENT_POSTS_LIMIT),
            pageAccessToken);

        List<Map<?, ?>> newComments = new ArrayList<>();

        for (Map<?, ?> post : FacebookPagesUtils.getDataList(postsBody)) {
            String postId = (String) post.get(ID);

            Map<String, Object> commentsBody = FacebookPagesUtils.executePageRequest(
                context.http(http -> http.get("/" + postId + "/comments"))
                    .queryParameters("fields", COMMENT_FIELDS, "order", "reverse_chronological", "limit", 100),
                pageAccessToken);

            for (Map<?, ?> comment : FacebookPagesUtils.getDataList(commentsBody)) {
                if (isCreatedBetween(comment, lastTimeChecked, now)) {
                    Map<Object, Object> newComment = new HashMap<>(comment);

                    newComment.put(POST_ID, postId);

                    newComments.add(newComment);
                }
            }
        }

        return new PollOutput(newComments, Map.of(LAST_TIME_CHECKED, now), false);
    }

    private static boolean isCreatedBetween(Map<?, ?> comment, LocalDateTime start, LocalDateTime end) {
        if (!(comment.get(CREATED_TIME) instanceof String createdTimeString)) {
            return false;
        }

        OffsetDateTime createdOffsetDateTime = OffsetDateTime.parse(createdTimeString, CREATED_TIME_FORMATTER);

        LocalDateTime createdTime = LocalDateTime.ofInstant(createdOffsetDateTime.toInstant(), ZoneOffset.UTC);

        return createdTime.isAfter(start) && !createdTime.isAfter(end);
    }
}
