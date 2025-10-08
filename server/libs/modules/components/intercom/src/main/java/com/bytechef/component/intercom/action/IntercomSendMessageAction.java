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

package com.bytechef.component.intercom.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.intercom.constant.IntercomConstants.BODY;
import static com.bytechef.component.intercom.constant.IntercomConstants.FROM;
import static com.bytechef.component.intercom.constant.IntercomConstants.ID;
import static com.bytechef.component.intercom.constant.IntercomConstants.MESSAGE_TYPE;
import static com.bytechef.component.intercom.constant.IntercomConstants.SUBJECT;
import static com.bytechef.component.intercom.constant.IntercomConstants.TEMPLATE;
import static com.bytechef.component.intercom.constant.IntercomConstants.TO;
import static com.bytechef.component.intercom.constant.IntercomConstants.TYPE;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.intercom.util.IntercomUtils;
import java.util.Map;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class IntercomSendMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendMessage")
        .title("Send Message")
        .description("Send a new message")
        .properties(
            string(MESSAGE_TYPE)
                .options(option("In App", "inapp"), option("Email", "email"))
                .label("Message Type")
                .description("In app message or email message")
                .required(true),
            string(SUBJECT)
                .label("Title")
                .description("Title of the Email/Message")
                .maxLength(360)
                .required(true),
            string(BODY)
                .label("Content")
                .description("Content of the message")
                .maxLength(360)
                .controlType(Property.ControlType.RICH_TEXT)
                .required(true),
            string(TEMPLATE)
                .options(option("Plain", "plain"), option("Personal", "personal"))
                .label("Template")
                .description("The style of the outgoing message")
                .required(true),
            string(TO)
                .label("To")
                .description("ID of the contact to send the message to.")
                .required(true)
                .options((OptionsFunction<String>) IntercomUtils::getContactIdOptions))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(TYPE)
                            .description("The type of the message."),
                        string(ID)
                            .description("ID of the message."),
                        string(SUBJECT)
                            .description("The subject of the message."),
                        string(BODY)
                            .description("The message body, which may contain HTML."),
                        string(MESSAGE_TYPE)
                            .description("The type of message that was sent."),
                        string("conversation_id")
                            .description("The associated conversation_id."))))
        .perform(IntercomSendMessageAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_MESSAGES_CONTEXT_FUNCTION =
        http -> http.post("/messages");

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, String> fromData = IntercomUtils.getContactRole(inputParameters.getString(TO), context);
        Map<String, String> toData = IntercomUtils.getAdminId(context);

        return context.http(POST_MESSAGES_CONTEXT_FUNCTION)
            .body(
                Body.of(
                    MESSAGE_TYPE, inputParameters.getRequiredString(MESSAGE_TYPE),
                    SUBJECT, inputParameters.getRequiredString(SUBJECT),
                    BODY, inputParameters.getRequiredString(BODY),
                    TEMPLATE, inputParameters.getRequiredString(TEMPLATE),
                    FROM, fromData,
                    TO, toData))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private IntercomSendMessageAction() {
    }
}
