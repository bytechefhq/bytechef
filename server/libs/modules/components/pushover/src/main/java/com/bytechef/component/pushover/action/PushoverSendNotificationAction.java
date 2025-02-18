/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.pushover.constant.PushoverConstants.MESSAGE;
import static com.bytechef.component.pushover.constant.PushoverConstants.TITLE;
import static com.bytechef.component.pushover.constant.PushoverConstants.TOKEN;
import static com.bytechef.component.pushover.constant.PushoverConstants.USER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;

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
                .required(true))
        .perform(PushoverSendNotificationAction::perform);

    private PushoverSendNotificationAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext
            .http(http -> http.post("/messages.json"))
            .body(Body.of(
                TOKEN, connectionParameters.getRequiredString(TOKEN),
                USER, connectionParameters.getRequiredString(USER),
                MESSAGE, inputParameters.getRequiredString(MESSAGE),
                TITLE, inputParameters.getString(TITLE)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return null;
    }
}
