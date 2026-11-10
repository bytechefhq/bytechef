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

package com.bytechef.component.loops.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.loops.constant.LoopsConstants.EMAIL_MESSAGE_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.EMAIL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.loops.constant.LoopsConstants.EXPECTED_REVISION_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.FROM_EMAIL;
import static com.bytechef.component.loops.constant.LoopsConstants.FROM_NAME;
import static com.bytechef.component.loops.constant.LoopsConstants.LMX;
import static com.bytechef.component.loops.constant.LoopsConstants.PREVIEW_TEXT;
import static com.bytechef.component.loops.constant.LoopsConstants.REPLY_TO_EMAIL;
import static com.bytechef.component.loops.constant.LoopsConstants.SUBJECT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LoopsUpdateEmailMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("updateEmailMessage")
        .title("Update Email Message")
        .description(
            "Update the subject, preview text, sender or LMX body of an email message. Only the fields you fill in " +
                "are changed. A campaign's email message can be updated only while the campaign is a draft.")
        .properties(
            string(EMAIL_MESSAGE_ID)
                .label("Email Message ID")
                .description("The ID of the email message to update.")
                .required(true),
            string(EXPECTED_REVISION_ID)
                .label("Expected Revision ID")
                .description(
                    "The content revision you last read: the Content Revision ID from Get Email Message, or the " +
                        "Email Message Content Revision ID from Create Campaign. The update is applied only if the " +
                        "email is still at this revision, so a newer edit is never overwritten.")
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("The email subject.")
                .required(false),
            string(PREVIEW_TEXT)
                .label("Preview Text")
                .description("The email preview text.")
                .required(false),
            string(FROM_NAME)
                .label("From Name")
                .description("The email sender name.")
                .required(false),
            string(FROM_EMAIL)
                .label("From Email")
                .description(
                    "The sender email address without the team's sending domain, for example hello for " +
                        "hello@yourdomain.com.")
                .required(false),
            string(REPLY_TO_EMAIL)
                .label("Reply-To Email")
                .description("The reply-to email address.")
                .required(false),
            string(LMX)
                .label("LMX")
                .description(
                    "The email body in LMX, Loops' XML-like email markup, for example " +
                        "<Paragraph>Hello, {firstName}!</Paragraph>. Styles go in the LMX <Style /> tag. The " +
                        "payload is limited to 100KB. See https://loops.so/docs/creating-emails/lmx for the format.")
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output(outputSchema(EMAIL_MESSAGE_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/loops_v1#update-email-message")
        .perform(LoopsUpdateEmailMessageAction::perform);

    private LoopsUpdateEmailMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.post("/email-messages/" + inputParameters.getRequiredString(EMAIL_MESSAGE_ID)))
            .body(
                Body.of(
                    EXPECTED_REVISION_ID, inputParameters.getRequiredString(EXPECTED_REVISION_ID),
                    SUBJECT, inputParameters.getString(SUBJECT),
                    PREVIEW_TEXT, inputParameters.getString(PREVIEW_TEXT),
                    FROM_NAME, inputParameters.getString(FROM_NAME),
                    FROM_EMAIL, inputParameters.getString(FROM_EMAIL),
                    REPLY_TO_EMAIL, inputParameters.getString(REPLY_TO_EMAIL),
                    LMX, inputParameters.getString(LMX)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
