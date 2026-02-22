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

package com.bytechef.component.youtube.trigger;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.youtube.constant.YouTubeConstants.IDENTIFIER;
import static com.bytechef.component.youtube.constant.YouTubeConstants.ITEMS;
import static com.bytechef.component.youtube.constant.YouTubeConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.youtube.constant.YouTubeConstants.SNIPPET;
import static com.bytechef.component.youtube.util.YouTubeUtils.getChannelId;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.google.commons.GoogleUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class YouTubeNewVideoTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newVideo")
        .title("New Video")
        .description("Triggers when new video is added to a specific channel.")
        .type(TriggerType.POLLING)
        .properties(
            string(IDENTIFIER)
                .label("Username/Channel Handle")
                .description("YouTube username or a channel handle (e.g. @Youtube).")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("publishedAt")
                            .description("The date and time when the video was published."),
                        string("channelId")
                            .description("ID of the channel where the video was uploaded."),
                        string("title")
                            .description("Title of the video."),
                        string("description")
                            .description("Description of the video."),
                        object("thumbnails")
                            .description("Video thumbnails of different quality.")
                            .properties(
                                object("default")
                                    .description("Default quality thumbnail of the video.")
                                    .properties(
                                        string("url")
                                            .description("URL of the thumbnail."),
                                        integer("width")
                                            .description("Width of the thumbnail."),
                                        integer("height")
                                            .description("Height of the thumbnail.")),
                                object("medium")
                                    .description("Medium quality thumbnail of the video.")
                                    .properties(
                                        string("url")
                                            .description("URL of the thumbnail."),
                                        integer("width")
                                            .description("Width of the thumbnail."),
                                        integer("height")
                                            .description("Height of the thumbnail.")),
                                object("high")
                                    .description("High quality thumbnail of the video.")
                                    .properties(
                                        string("url")
                                            .description("URL of the thumbnail."),
                                        integer("width")
                                            .description("Width of the thumbnail."),
                                        integer("height")
                                            .description("Height of the thumbnail."))),
                        string("channelTitle")
                            .description("Title of the channel."),
                        string("liveBroadcastContent")
                            .description("Live broadcasting content."),
                        string("publishTime")
                            .description("The date and time when the video was published."))))
        .poll(YouTubeNewVideoTrigger::poll)
        .processErrorResponse(GoogleUtils::processErrorResponse)
        .help("", "https://docs.bytechef.io/reference/components/youtube_v1#new-video");

    private YouTubeNewVideoTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        String channelId = getChannelId(inputParameters.getRequiredString(IDENTIFIER), triggerContext);
        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, triggerContext.isEditorEnvironment() ? now.minusHours(3) : now);

        ZonedDateTime startZonedDate = startDate.atZone(zoneId);

        Map<String, Object> response =
            triggerContext.http(http -> http.get("/search"))
                .queryParameters(
                    "part", SNIPPET,
                    "channelId", channelId,
                    "type", "video",
                    "order", "date",
                    "publishedAfter", startZonedDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .configuration(Http.responseType(ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

        List<Map<?, ?>> newVideos = new ArrayList<>();

        if (response.get(ITEMS) instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> itemMap &&
                    itemMap.get(SNIPPET) instanceof Map<?, ?> snippet) {

                    newVideos.add(snippet);
                }
            }
        }

        return new PollOutput(newVideos, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
