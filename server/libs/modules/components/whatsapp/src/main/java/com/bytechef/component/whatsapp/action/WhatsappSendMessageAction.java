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

package com.bytechef.component.whatsapp.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.BASE_URL;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.BODY;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.MESSAGING_PRODUCT;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.PHONE_NUMBER_ID;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.RECEIVE_USER;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.RECIPIENT_TYPE;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.SEND_MESSAGE;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.SYSTEM_USER_ACCESS_TOKEN;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.TEXT;
import static com.bytechef.component.whatsapp.constant.WhatsappConstants.TYPE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

public class WhatsappSendMessageAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_MESSAGE)
        .title("Send Message")
        .description("Send a message via Whatsapp")
        .properties(
            string(BODY)
                .label("Message")
                .description("Message to send via whatsapp")
                .maxLength(4096)
                .required(true),
            string(RECEIVE_USER)
                .label("Send message to")
                .description("Phone number to send the message. It must start with \"+\" sign")
                .required(true))
        .outputSchema(
            object()
                .properties())
        .perform(WhatsappSendMessageAction::perform);

    private WhatsappSendMessageAction() {
    }

    public static Object
        perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        return actionContext
            .http(http -> http.post(BASE_URL + "/" + connectionParameters.getString(PHONE_NUMBER_ID) + "/messages"))
            .header("Authorization", "Bearer " + connectionParameters.getString(SYSTEM_USER_ACCESS_TOKEN))
            .body(
                Body.of(
                    MESSAGING_PRODUCT, "whatsapp",
                    RECIPIENT_TYPE, "individual",
                    RECEIVE_USER, inputParameters.getRequiredString(RECEIVE_USER),
                    TYPE, "text",
                    TEXT, Map.of(BODY, inputParameters.getRequiredString(BODY))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
