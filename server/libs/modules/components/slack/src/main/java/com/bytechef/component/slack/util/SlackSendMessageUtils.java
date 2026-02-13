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

package com.bytechef.component.slack.util;

import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.ERROR;
import static com.bytechef.component.slack.constant.SlackConstants.OK;
import static com.bytechef.component.slack.constant.SlackConstants.POST_AT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.util.SlackUtils.getSlackTimeZone;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class SlackSendMessageUtils {

    private SlackSendMessageUtils() {
    }

    public static Object sendMessage(
        String channel, String text, LocalDateTime postAt, List<Map<String, Object>> blocks, Context context) {

        Map<String, Object> response = postAt == null ? sendMessageNormal(channel, text, blocks, context)
            : sendMessageScheduled(channel, text, postAt, blocks, context);

        if ((boolean) response.get(OK)) {
            return response;
        } else {
            throw new ProviderException((String) response.get(ERROR));
        }
    }

    private static Map<String, Object> sendMessageNormal(
        String channel, String text, List<Map<String, Object>> blocks, Context context) {

        return context
            .http(http -> http.post("/chat.postMessage"))
            .body(
                Http.Body.of(
                    CHANNEL, channel,
                    TEXT, text,
                    "blocks", blocks))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Object> sendMessageScheduled(
        String channel, String text, LocalDateTime schedule, List<Map<String, Object>> blocks, Context context) {

        String timeZone = getSlackTimeZone(context);
        ZonedDateTime zonedSchedule = schedule.atZone(ZoneId.of(timeZone));
        long seconds = zonedSchedule.toEpochSecond();

        return context
            .http(http -> http.post("/chat.scheduleMessage"))
            .body(
                Http.Body.of(
                    CHANNEL, channel,
                    TEXT, text,
                    POST_AT, seconds,
                    "blocks", blocks))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
