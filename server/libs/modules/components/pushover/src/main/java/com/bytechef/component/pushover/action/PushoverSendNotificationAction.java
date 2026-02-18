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

package com.bytechef.component.pushover.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pushover.constant.PushoverConstants.ATTACHMENT_BASE_64;
import static com.bytechef.component.pushover.constant.PushoverConstants.EXPIRE;
import static com.bytechef.component.pushover.constant.PushoverConstants.MESSAGE;
import static com.bytechef.component.pushover.constant.PushoverConstants.PRIORITY;
import static com.bytechef.component.pushover.constant.PushoverConstants.REQUEST;
import static com.bytechef.component.pushover.constant.PushoverConstants.RETRY;
import static com.bytechef.component.pushover.constant.PushoverConstants.STATUS;
import static com.bytechef.component.pushover.constant.PushoverConstants.TITLE;
import static com.bytechef.component.pushover.constant.PushoverConstants.TOKEN;
import static com.bytechef.component.pushover.constant.PushoverConstants.URL;
import static com.bytechef.component.pushover.constant.PushoverConstants.URL_TITLE;
import static com.bytechef.component.pushover.constant.PushoverConstants.USER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.pushover.util.PushoverUtils;

/**
 * @author Nikolina Spehar
 */
public class PushoverSendNotificationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendNotification")
        .title("Send Notification")
        .description("Sends a notification.")
        .properties(
            string(TITLE)
                .label("Message Title")
                .description("The title of the message that will be sent.")
                .required(false),
            string(MESSAGE)
                .label("Message")
                .description("The message to send.")
                .required(true),
            string(PRIORITY)
                .label("Priority")
                .description("The priority of the message.")
                .options(PushoverUtils.getMessagePriorityOptions())
                .required(false),
            integer(RETRY)
                .label("Retry")
                .description(
                    "How often will the notification be sent to the user. Must have a value of at least 30 seconds")
                .displayCondition("%s == '%s'".formatted(PRIORITY, "2"))
                .required(true),
            integer(EXPIRE)
                .label("Expire")
                .description(
                    "If the notification has not be acknowledged in expire seconds, it will be marked as expired and " +
                        "will stop being sent to the user.")
                .displayCondition("%s == '%s'".formatted(PRIORITY, "2"))
                .required(true),
            string(URL)
                .label("URL")
                .description("Clickable URL link in the message to send.")
                .required(false),
            string(URL_TITLE)
                .label("Url Title")
                .description(
                    "When the user taps on the notification in Pushover to expand it, the URL will be shown as the " +
                        "supplied url_title")
                .required(false),
            fileEntry(ATTACHMENT_BASE_64)
                .label("Attachment")
                .description("The attachment to send.")
                .required(false))
        .output(outputSchema(
            object()
                .properties(
                    integer(STATUS),
                    string(REQUEST))))
        .perform(PushoverSendNotificationAction::perform);

    private PushoverSendNotificationAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        FileEntry fileEntry = inputParameters.getFileEntry(ATTACHMENT_BASE_64);
        String attachmentBase64 = null;

        if (fileEntry != null) {
            byte[] fileContent =
                actionContext.file(file -> file.readAllBytes(fileEntry));

            attachmentBase64 = actionContext.encoder(encoder -> encoder.base64Encode(fileContent));
        }

        return actionContext
            .http(http -> http.post("/messages.json"))
            .body(
                Body.of(
                    TOKEN, connectionParameters.getRequiredString(TOKEN),
                    USER, connectionParameters.getRequiredString(USER),
                    MESSAGE, inputParameters.getRequiredString(MESSAGE),
                    TITLE, inputParameters.getString(TITLE),
                    PRIORITY, inputParameters.getString(PRIORITY),
                    RETRY, inputParameters.getInteger(RETRY),
                    EXPIRE, inputParameters.getInteger(EXPIRE),
                    URL, inputParameters.getString(URL),
                    URL_TITLE, inputParameters.getString(URL_TITLE),
                    ATTACHMENT_BASE_64, attachmentBase64))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
