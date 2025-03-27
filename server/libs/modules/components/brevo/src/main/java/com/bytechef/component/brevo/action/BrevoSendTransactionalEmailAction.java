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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.NAME;
import static com.bytechef.component.brevo.constant.BrevoConstants.SUBJECT;
import static com.bytechef.component.brevo.constant.BrevoConstants.TEXT_CONTENT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.brevo.util.BrevoUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrevoSendTransactionalEmailAction {

    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("sendTransactionalEmail")
        .title("Send transactional email")
        .description("Send a transactional email.")
        .properties(
            string("senderEmail")
                .label("Sender email")
                .description("Email of the sender from which the emails will be sent.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BrevoUtils::getSendersOptions)
                .required(true),
            string("senderName")
                .label("Sender name")
                .description("Name of the sender from which the emails will be sent.")
                .required(true),
            string("recipientEmail")
                .label("Recipient email")
                .description("Email address of the recipient.")
                .options((OptionsDataSource.ActionOptionsFunction<String>) BrevoUtils::getContactsOptions)
                .required(true),
            string("recipientName")
                .label("Recipient name")
                .description("Name of the recipient.")
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("Subject of the message.")
                .required(false),
            string(TEXT_CONTENT)
                .label("Text body")
                .description("Plain text body of the message.")
                .required(false))
        .output(
            outputSchema(
                string()
                    .description("Message ID of the transactional email sent.")))
        .perform(BrevoSendTransactionalEmailAction::perform);

    private BrevoSendTransactionalEmailAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context
            .http(http -> http.post("/smtp/email/"))
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .body(Body.of(
                "sender", Map.of(
                    EMAIL, inputParameters.getRequiredString("senderEmail"),
                    NAME, inputParameters.getRequiredString("senderName")),
                "to", List.of(Map.of(
                    EMAIL, inputParameters.getRequiredString("recipientEmail"),
                    NAME, inputParameters.getRequiredString("recipientName"))),
                SUBJECT, inputParameters.getRequiredString(SUBJECT),
                TEXT_CONTENT, inputParameters.getString(TEXT_CONTENT)))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
