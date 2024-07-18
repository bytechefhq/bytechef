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

package com.bytechef.component.resend.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.resend.constant.ResendConstants.ATTACHMENTS;
import static com.bytechef.component.resend.constant.ResendConstants.BASE_URL;
import static com.bytechef.component.resend.constant.ResendConstants.BCC;
import static com.bytechef.component.resend.constant.ResendConstants.CC;
import static com.bytechef.component.resend.constant.ResendConstants.CONTENT_TYPE;
import static com.bytechef.component.resend.constant.ResendConstants.EMAIL_PROPERTY;
import static com.bytechef.component.resend.constant.ResendConstants.FROM;
import static com.bytechef.component.resend.constant.ResendConstants.HEADERS;
import static com.bytechef.component.resend.constant.ResendConstants.HTML;
import static com.bytechef.component.resend.constant.ResendConstants.NAME;
import static com.bytechef.component.resend.constant.ResendConstants.REPLY_TO;
import static com.bytechef.component.resend.constant.ResendConstants.SEND_EMAIL;
import static com.bytechef.component.resend.constant.ResendConstants.SUBJECT;
import static com.bytechef.component.resend.constant.ResendConstants.TAGS;
import static com.bytechef.component.resend.constant.ResendConstants.TEXT;
import static com.bytechef.component.resend.constant.ResendConstants.TO;
import static com.bytechef.component.resend.constant.ResendConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.resend.util.ResendUtils;
import java.util.List;

/**
 * @author Monika Domiter
 */
public final class ResendSendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_EMAIL)
        .title("Send Email")
        .description("Send an email")
        .properties(
            string(FROM)
                .label("From")
                .description("Sender email address.")
                .controlType(Property.ControlType.EMAIL)
                .required(true),
            array(TO)
                .label("To")
                .description("Recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .maxItems(50)
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("Email subject.")
                .required(true),
            array(BCC)
                .label("Bcc")
                .description("Bcc recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .required(false),
            array(CC)
                .label("Cc")
                .description("Cc recipients email addresses.")
                .items(EMAIL_PROPERTY)
                .required(false),
            array(REPLY_TO)
                .label("Reply to")
                .description("Reply-to email addresses.")
                .items(EMAIL_PROPERTY)
                .required(false),
            integer(CONTENT_TYPE)
                .label("Content type")
                .options(
                    option("HTML", 1),
                    option("Plain text", 2))
                .defaultValue(1)
                .required(true),
            string(HTML)
                .label("HTML")
                .description("The HTML version of the message.")
                .displayCondition("%s == %s".formatted(CONTENT_TYPE, 1))
                .required(false),
            string(TEXT)
                .label("Text")
                .description("The plain text version of the message.")
                .displayCondition("%s == %s".formatted(CONTENT_TYPE, 2))
                .required(false),
            object(HEADERS)
                .label("Headers")
                .description("Custom headers to add to the email.")
                .additionalProperties(string())
                .required(false),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry())
                .required(false),
            array(TAGS)
                .items(
                    object()
                        .properties(
                            string(NAME)
                                .label("Name")
                                .description("The name of the email tag.")
                                .maxLength(256)
                                .required(true),
                            string(VALUE)
                                .label("Value")
                                .description("The value of the email tag.")
                                .maxLength(256)
                                .required(true)))
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string("id")))
        .perform(ResendSendEmailAction::perform);

    private ResendSendEmailAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/emails"))
            .body(
                Http.Body.of(
                    FROM, inputParameters.getRequiredString(FROM),
                    TO, inputParameters.getRequiredList(TO, String.class),
                    SUBJECT, inputParameters.getRequiredString(SUBJECT),
                    BCC, inputParameters.getList(BCC, String.class),
                    CC, inputParameters.getList(CC, String.class),
                    REPLY_TO, inputParameters.getList(REPLY_TO, String.class),
                    HTML, inputParameters.getString(HTML),
                    TEXT, inputParameters.getString(TEXT),
                    HEADERS, inputParameters.getMap(HEADERS, String.class),
                    ATTACHMENTS,
                    ResendUtils.getAttachments(inputParameters.getFileEntries(ATTACHMENTS, List.of()), actionContext),
                    TAGS, inputParameters.getList(TAGS)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
