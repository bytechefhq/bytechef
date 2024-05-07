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

package com.bytechef.component.intercom.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.intercom.constant.IntercomConstants.BASE_URL;
import static com.bytechef.component.intercom.constant.IntercomConstants.BODY;
import static com.bytechef.component.intercom.constant.IntercomConstants.FROM;
import static com.bytechef.component.intercom.constant.IntercomConstants.MESSAGE_TYPE;
import static com.bytechef.component.intercom.constant.IntercomConstants.SUBJECT;
import static com.bytechef.component.intercom.constant.IntercomConstants.TEMPLATE;
import static com.bytechef.component.intercom.constant.IntercomConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.intercom.constant.IntercomConstants;
import com.bytechef.component.intercom.util.IntercomOptionUtils;
import com.bytechef.component.intercom.util.IntercomUtils;
import java.util.Map;

public class IntercomSendMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(IntercomConstants.SEND_MESSAGE)
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
                .required(true),
            string(TEMPLATE)
                .options(option("Plain", "plain"), option("Personal", "personal"))
                .label("Template")
                .description("The style of the outgoing message")
                .required(true),
            string(TO)
                .label("To")
                .description("Receiver of the message")
                .required(true)
                .options(
                    (OptionsDataSource.ActionOptionsFunction<String>) IntercomOptionUtils::getContactIdOptions))
        .outputSchema(
            object())
        .perform(IntercomSendMessageAction::perform);

    static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {
        Map<String, String> fromData = IntercomUtils.getContactRole(inputParameters.getString(TO), actionContext);
        Map<String, String> toData = IntercomUtils.getAdminId(actionContext);

        return actionContext.http(http -> http.post(BASE_URL + "/messages"))
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
            .getBody(new Context.TypeReference<>() {});
    }

    private IntercomSendMessageAction() {
    }
}
