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

package com.bytechef.component.viber.util;

import static com.bytechef.component.viber.constant.ViberConstants.CONVERSATION_STARTED;
import static com.bytechef.component.viber.constant.ViberConstants.MESSAGE;
import static com.bytechef.component.viber.constant.ViberConstants.SUBSCRIBED;
import static com.bytechef.component.viber.constant.ViberConstants.UNSUBSCRIBED;
import static com.bytechef.component.viber.constant.ViberConstants.WEBHOOK;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TriggerContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class ViberEventUtils {

    private static final Logger log = LoggerFactory.getLogger(ViberEventUtils.class);

    private static final Set<String> MEDIA_MESSAGE_TYPES = Set.of("picture", "video", "file");

    private ViberEventUtils() {
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> toOutput(
        Map<String, Object> event, Set<String> selectedEvents, TriggerContext context) {

        String eventName = (String) event.get("event");

        if (eventName == null || WEBHOOK.equals(eventName) || !selectedEvents.contains(eventName)) {
            return List.of();
        }

        Map<String, Object> output = new HashMap<>();

        output.put("event", eventName);

        putIfPresent(output, "timestamp", event.get("timestamp"));
        putIfPresent(output, "message_token", event.get("message_token"));
        putIfPresent(output, "user", resolveUser(eventName, event));

        Object message = event.get(MESSAGE);

        putIfPresent(output, MESSAGE, message);

        if (CONVERSATION_STARTED.equals(eventName)) {
            putIfPresent(output, "type", event.get("type"));
            putIfPresent(output, "context", event.get("context"));
            putIfPresent(output, SUBSCRIBED, event.get(SUBSCRIBED));
        }

        if (message instanceof Map<?, ?> messageMap) {
            FileEntry fileEntry = downloadMedia((Map<String, Object>) messageMap, context);

            putIfPresent(output, "file", fileEntry);
        }

        return List.of(output);
    }

    private static FileEntry downloadMedia(Map<String, Object> message, TriggerContext context) {
        Object type = message.get("type");
        Object media = message.get("media");

        if (!(type instanceof String messageType) || !MEDIA_MESSAGE_TYPES.contains(messageType) ||
            !(media instanceof String mediaUrl)) {

            return null;
        }

        try {
            Http.Response response = context.http(http -> http.get(mediaUrl))
                .configuration(
                    Http.responseType(Http.ResponseType.BINARY)
                        .disableAuthorization(true))
                .execute();

            return (FileEntry) response.getBody();
        } catch (Exception exception) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to download Viber media from host {}", getHost(mediaUrl), exception);
            }

            return null;
        }
    }

    private static @Nullable String getHost(String mediaUrl) {
        try {
            URI uri = new URI(mediaUrl);

            return uri.getHost();
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private static Object resolveUser(String eventName, Map<String, Object> event) {
        if (MESSAGE.equals(eventName)) {
            return event.get("sender");
        }

        if (UNSUBSCRIBED.equals(eventName)) {
            Object userId = event.get("user_id");

            return userId == null ? null : Map.of("id", userId);
        }

        return event.get("user");
    }

    private static void putIfPresent(Map<String, Object> output, String key, Object value) {
        if (value != null) {
            output.put(key, value);
        }
    }
}
