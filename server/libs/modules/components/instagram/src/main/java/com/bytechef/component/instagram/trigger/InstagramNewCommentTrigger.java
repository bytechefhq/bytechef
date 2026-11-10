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

package com.bytechef.component.instagram.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.instagram.constant.InstagramConstants.createCommentOutputProperty;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.instagram.util.InstagramUtils;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class InstagramNewCommentTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";
    protected static final int RECENT_MEDIA_LIMIT = 10;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd'T'HH:mm:ss[XXX][XX][X]");

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newComment")
        .title("New Comment")
        .description(
            "Triggers when someone comments on one of the 10 most recent media items of the connected Instagram " +
                "professional account.")
        .help("", "https://docs.bytechef.io/reference/components/instagram_v1#new-comment")
        .type(TriggerType.POLLING)
        .output(
            outputSchema(
                array()
                    .items(
                        createCommentOutputProperty(
                            string("media_id")
                                .description("ID of the media item the comment was posted on.")))))
        .poll(InstagramNewCommentTrigger::poll);

    private InstagramNewCommentTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC)
            .truncatedTo(ChronoUnit.SECONDS);

        LocalDateTime lastTimeChecked = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        String userId = InstagramUtils.getUserId(context);

        Map<String, Object> mediaResponse = context.http(http -> http.get("/" + userId + "/media"))
            .queryParameters("fields", "id,timestamp", "limit", RECENT_MEDIA_LIMIT)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<String, Object>> newComments = new ArrayList<>();

        for (Map<String, Object> media : InstagramUtils.getData(mediaResponse)) {
            String mediaId = String.valueOf(media.get("id"));

            for (Map<String, Object> comment : InstagramUtils.getComments(context, mediaId)) {
                if (isCreatedAfter(comment, lastTimeChecked)) {
                    Map<String, Object> newComment = new LinkedHashMap<>(comment);

                    newComment.put("media_id", mediaId);

                    newComments.add(newComment);
                }
            }
        }

        return new PollOutput(newComments, Map.of(LAST_TIME_CHECKED, now), false);
    }

    private static boolean isCreatedAfter(Map<String, Object> comment, LocalDateTime lastTimeChecked) {
        if (!(comment.get("timestamp") instanceof String timestamp)) {
            return false;
        }

        OffsetDateTime createdAt = OffsetDateTime.parse(timestamp, TIMESTAMP_FORMATTER);

        LocalDateTime createdAtUtc = LocalDateTime.ofInstant(createdAt.toInstant(), ZoneOffset.UTC);

        return createdAtUtc.isAfter(lastTimeChecked);
    }
}
