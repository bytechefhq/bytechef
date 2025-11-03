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

package com.bytechef.component.telegram.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.telegram.constant.TelegramConstants.CHAT_ID;
import static com.bytechef.component.telegram.constant.TelegramConstants.DIRECT_MESSAGES_TOPIC_ID;
import static com.bytechef.component.telegram.constant.TelegramConstants.DOCUMENT;
import static com.bytechef.component.telegram.constant.TelegramConstants.MEDIA_TYPE;
import static com.bytechef.component.telegram.constant.TelegramConstants.MESSAGE_OUTPUT_PROPERTIES;
import static com.bytechef.component.telegram.constant.TelegramConstants.PHOTO;
import static com.bytechef.component.telegram.constant.TelegramConstants.VIDEO;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class TelegramSendMediaAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendMedia")
        .title("Send Media")
        .description("Sends a media message through a Telegram bot.")
        .properties(
            string(CHAT_ID)
                .label("Chat ID")
                .description("Unique identifier for the target chat or username of the target channel.")
                .required(true),
            string(MEDIA_TYPE)
                .description("Type of media to send.")
                .options(
                    option("Document", DOCUMENT),
                    option("Photo", PHOTO),
                    option("Video", VIDEO))
                .required(true),
            fileEntry(DOCUMENT)
                .label("Document")
                .description("Document to send.")
                .displayCondition("%s == '%s'".formatted(MEDIA_TYPE, DOCUMENT))
                .required(true),
            fileEntry(PHOTO)
                .label("Photo")
                .description("Photo to send.")
                .displayCondition("%s == '%s'".formatted(MEDIA_TYPE, PHOTO))
                .required(true),
            fileEntry(VIDEO)
                .label("Video")
                .description("Video to send.")
                .displayCondition("%s == '%s'".formatted(MEDIA_TYPE, VIDEO))
                .required(true),
            string(DIRECT_MESSAGES_TOPIC_ID)
                .label("Direct Messages Topic ID")
                .description(
                    "Identifier of the direct messages topic to which the message will be sent; required if the " +
                        "message is sent to a direct messages chat.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("ok"),
                        object("result")
                            .properties(MESSAGE_OUTPUT_PROPERTIES))))
        .perform(TelegramSendMediaAction::perform);

    private TelegramSendMediaAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String mediaType = inputParameters.getRequiredString(MEDIA_TYPE);

        String url;
        Map<String, FileEntry> bodyMap;

        switch (mediaType) {
            case DOCUMENT -> {
                url = "/sendDocument";
                bodyMap = Map.of(DOCUMENT, inputParameters.getRequiredFileEntry(DOCUMENT));
            }
            case PHOTO -> {
                url = "/sendPhoto";
                bodyMap = Map.of(PHOTO, inputParameters.getRequiredFileEntry(PHOTO));
            }
            case VIDEO -> {
                url = "/sendVideo";
                bodyMap = Map.of(VIDEO, inputParameters.getRequiredFileEntry(VIDEO));
            }
            default -> throw new IllegalArgumentException("Unsupported media type: " + mediaType);
        }

        return context.http(http -> http.post(url))
            .queryParameters(
                CHAT_ID, inputParameters.getRequiredString(CHAT_ID),
                DIRECT_MESSAGES_TOPIC_ID, inputParameters.getString(DIRECT_MESSAGES_TOPIC_ID))
            .body(Http.Body.of(bodyMap, Http.BodyContentType.FORM_DATA))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
