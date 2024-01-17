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

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.resend.constant.ResendConstants.ATTACHMENTS;
import static com.bytechef.component.resend.constant.ResendConstants.BCC;
import static com.bytechef.component.resend.constant.ResendConstants.CC;
import static com.bytechef.component.resend.constant.ResendConstants.EMAIL;
import static com.bytechef.component.resend.constant.ResendConstants.EMAIL_ADDRESS;
import static com.bytechef.component.resend.constant.ResendConstants.FROM;
import static com.bytechef.component.resend.constant.ResendConstants.HEADERS;
import static com.bytechef.component.resend.constant.ResendConstants.HTML;
import static com.bytechef.component.resend.constant.ResendConstants.NAME;
import static com.bytechef.component.resend.constant.ResendConstants.REACT;
import static com.bytechef.component.resend.constant.ResendConstants.REPLY_TO;
import static com.bytechef.component.resend.constant.ResendConstants.SEND_EMAIL;
import static com.bytechef.component.resend.constant.ResendConstants.SUBJECT;
import static com.bytechef.component.resend.constant.ResendConstants.TAGS;
import static com.bytechef.component.resend.constant.ResendConstants.TEXT;
import static com.bytechef.component.resend.constant.ResendConstants.TO;
import static com.bytechef.component.resend.constant.ResendConstants.VALUE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.resend.util.ResendUtils;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Tag;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public final class ResendSendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEND_EMAIL)
        .title("Send Email")
        .description("Description")
        .properties(
            string(FROM)
                .label("From")
                .description("Sender email address.")
                .controlType(Property.ControlType.EMAIL)
                .required(true),
            array(TO)
                .label("To")
                .description("Recipients email addresses.")
                .items(
                    string(EMAIL)
                        .label(EMAIL_ADDRESS)
                        .controlType(Property.ControlType.EMAIL))
                .maxItems(50)
                .required(true),
            string(SUBJECT)
                .label("Subject")
                .description("Email subject.")
                .required(false),
            array(BCC)
                .label("Bcc")
                .description("Bcc recipients email addresses.")
                .items(
                    string(EMAIL)
                        .label(EMAIL_ADDRESS)
                        .controlType(Property.ControlType.EMAIL))
                .required(false),
            array(CC)
                .label("Cc")
                .description("Cc recipients email addresses.")
                .items(
                    string(EMAIL)
                        .label(EMAIL_ADDRESS)
                        .controlType(Property.ControlType.EMAIL))
                .required(false),
            array(REPLY_TO)
                .label("Reply to")
                .description("Reply-to email addresses.")
                .items(
                    string(EMAIL)
                        .label(EMAIL_ADDRESS)
                        .controlType(Property.ControlType.EMAIL))
                .required(false),
            string(HTML)
                .label("HTML")
                .description("The HTML version of the message.")
                .required(false),
            string(TEXT)
                .label("Text")
                .description("The plain text version of the message.")
                .required(false),
            string(REACT)
                .label("React")
                .description("The React component used to write the message. Only available in the Node.js SDK.")
                .required(false),
            object(HEADERS)
                .label("Headers")
                .description("Custom headers to add to the email.")
                .additionalProperties(
                    string())
                .required(false),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments to send with the email.")
                .items(fileEntry())
                .required(false),
            array(TAGS)
                .items(
                    string(NAME)
                        .label("Name")
                        .description("The name of the email tag.")
                        .maxLength(256)
                        .required(true),
                    string(VALUE)
                        .label("Value")
                        .description("The value of the email tag.")
                        .maxLength(256)
                        .required(true))
                .required(false))
        .outputSchema(
            object()
                .properties(
                    string("id")))
        .perform(ResendSendEmailAction::perform);

    private ResendSendEmailAction() {
    }

    public static CreateEmailResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext)
        throws ResendException {

        Resend resend = new Resend(connectionParameters.get(TOKEN, String.class));

        List<Attachment> attachments = ResendUtils.getAttachments(inputParameters, actionContext);

        CreateEmailOptions createEmailOptions = CreateEmailOptions.builder()
            .from(inputParameters.getRequiredString(FROM))
            .to(inputParameters.getList(TO, String.class, List.of()))
            .subject(inputParameters.getString(SUBJECT))
            .bcc(inputParameters.getList(BCC, String.class, List.of()))
            .cc(inputParameters.getList(CC, String.class, List.of()))
            .replyTo(inputParameters.getList(REPLY_TO, String.class, List.of()))
            .html(inputParameters.getString(HTML))
            .text(inputParameters.getString(TEXT))
            .attachments(attachments)
            .headers(inputParameters.getMap(HEADERS, String.class, Map.of()))
            .tags(inputParameters.getList(TAGS, Tag.class, List.of()))
            .build();

        Emails emails = resend.emails();

        return emails.send(createEmailOptions);
    }
}
