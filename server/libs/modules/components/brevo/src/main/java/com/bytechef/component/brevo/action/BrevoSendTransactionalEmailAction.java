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

package com.bytechef.component.brevo.action;

import static com.bytechef.component.brevo.constant.BrevoConstants.BCC;
import static com.bytechef.component.brevo.constant.BrevoConstants.CC;
import static com.bytechef.component.brevo.constant.BrevoConstants.CONTENT;
import static com.bytechef.component.brevo.constant.BrevoConstants.CONTENT_TYPE;
import static com.bytechef.component.brevo.constant.BrevoConstants.EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.SENDER_EMAIL;
import static com.bytechef.component.brevo.constant.BrevoConstants.SUBJECT;
import static com.bytechef.component.brevo.constant.BrevoConstants.TO;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.brevo.util.BrevoUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BrevoSendTransactionalEmailAction {

    enum ContentType {
        HTML, TEXT
    }

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendTransactionalEmail")
        .title("Send Transactional Email")
        .description("Sends and email from your Brevo account.")
        .properties(
            string(SENDER_EMAIL)
                .label("Sender Email")
                .description("Email of the sender from which the emails will be sent.")
                .options((OptionsFunction<String>) BrevoUtils::getSendersOptions)
                .required(true),
            array(TO)
                .label("To Recipients")
                .description("The To: recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .options((OptionsFunction<String>) BrevoUtils::getContactsOptions)
                .minItems(1)
                .required(true),
            array(BCC)
                .label("Bcc Recipients")
                .description("The Bcc recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .options((OptionsFunction<String>) BrevoUtils::getContactsOptions)
                .required(false),
            array(CC)
                .label("Cc Recipients")
                .description("The Cc recipients for the message.")
                .items(string().controlType(ControlType.EMAIL))
                .options((OptionsFunction<String>) BrevoUtils::getContactsOptions)
                .required(false),
            string(SUBJECT)
                .label("Subject")
                .description("Subject of the email.")
                .required(true),
            string(CONTENT_TYPE)
                .label("Content Type")
                .description("Content type of the email.")
                .options(
                    option("Text", ContentType.TEXT.name()),
                    option("HTML", ContentType.HTML.name()))
                .defaultValue(ContentType.TEXT.name())
                .required(true),
            string(CONTENT)
                .label("Text Content")
                .description("Plain text body of the message.")
                .controlType(ControlType.TEXT_AREA)
                .displayCondition("contentType == '%s'".formatted(ContentType.TEXT))
                .required(true),
            string(CONTENT)
                .label("HTML Content")
                .description("HTML body of the message.")
                .controlType(ControlType.RICH_TEXT)
                .displayCondition("contentType == '%s'".formatted(ContentType.HTML))
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("messageId")
                            .description("Message ID of the transactional email sent."))))
        .perform(BrevoSendTransactionalEmailAction::perform);

    private BrevoSendTransactionalEmailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String type = "textContent";
        String contentType = inputParameters.getRequiredString(CONTENT_TYPE);

        if (contentType.equals(ContentType.HTML.name())) {
            type = "htmlContent";
        }

        return context
            .http(http -> http.post("/smtp/email/"))
            .body(Body.of(
                "sender", Map.of(EMAIL, inputParameters.getRequiredString(SENDER_EMAIL)),
                TO, getRecipients(inputParameters.getRequiredList(TO, String.class)),
                BCC, getRecipients(inputParameters.getList(BCC, String.class)),
                CC, getRecipients(inputParameters.getList(CC, String.class)),
                SUBJECT, inputParameters.getRequiredString(SUBJECT),
                type, inputParameters.getRequiredString(CONTENT)))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static List<Map<String, String>> getRecipients(List<String> recipients) {
        if (recipients == null) {
            return null;
        }

        return recipients
            .stream()
            .map(recipient -> Map.of(EMAIL, recipient))
            .toList();
    }
}
