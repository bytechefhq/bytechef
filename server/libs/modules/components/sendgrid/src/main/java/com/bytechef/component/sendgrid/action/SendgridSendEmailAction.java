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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ATTACHMENTS;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.BCC;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CC;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CONTENT_TYPE;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CONTENT_VALUE;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.DYNAMIC_TEMPLATE;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.EMAIL_PROPERTY;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.FROM;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.REPLY_TO;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SENDEMAIL;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUBJECT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TEMPLATE_ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.sendgrid.util.SendgridUtils;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Krišković
 */
public final class SendgridSendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SENDEMAIL)
        .title("Send an email")
        .description("Sends an email.")
        .properties(
            string(FROM)
                .label("From:")
                .description("Email address from which you want to send.")
                .controlType(Property.ControlType.EMAIL)
                .maxLength(320)
                .required(true),
            array(TO)
                .label("To:")
                .description("Email address which you want to send to.")
                .items(EMAIL_PROPERTY)
                .maxItems(1000)
                .required(true),
            string(REPLY_TO)
                .label("Reply to:")
                .description("Email address which you want to reply to.")
                .controlType(Property.ControlType.EMAIL)
                .maxLength(320)
                .required(false),
            array(CC)
                .label("CC:")
                .description("Email address which receives a copy.")
                .items(EMAIL_PROPERTY)
                .maxItems(1000)
                .required(false),
            array(BCC)
                .label("BCC:")
                .description("Email address which receives a private copy.")
                .items(EMAIL_PROPERTY)
                .maxItems(1000)
                .required(false),
            string(SUBJECT)
                .label("Subject")
                .description("Subject of your email")
                .minLength(1)
                .maxLength(998)
                .required(true),
            string(CONTENT_VALUE)
                .label("Message Body")
                .description("This is the message you want to send")
                .minLength(1)
                .required(true),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments you want to include with the email.")
                .items(fileEntry())
                .required(false),
            string(TEMPLATE_ID)
                .label("Template id")
                .description("ID of a template (alphanumeric).")
                .options((ActionOptionsFunction<String>) SendgridUtils::getTemplates)
                .required(false),
            object(DYNAMIC_TEMPLATE)
                .label("Dynamic Template")
                .description("Enter a dynamic template")
                .additionalProperties(object(), string())
                .required(false))
        .outputSchema(
            object()
                .properties(
                    integer("statusCode"),
                    string("body"),
                    object("headers")
                        .properties(
                            string("key"),
                            string("value"))))
        .perform(SendgridSendEmailAction::perform);

    private SendgridSendEmailAction() {
    }

    public static Response perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Mail mail = new Mail();

        mail.setFrom(new Email(inputParameters.getRequiredString(FROM)));
        mail.setSubject(inputParameters.getRequiredString(SUBJECT));
        mail.addContent(new Content(CONTENT_TYPE, inputParameters.getRequiredString(CONTENT_VALUE)));

        String templateId = inputParameters.getString(TEMPLATE_ID);

        if (templateId != null) {
            mail.setTemplateId(templateId);
        }

        String replyTo = inputParameters.getString(REPLY_TO);

        if (replyTo != null) {
            mail.setReplyTo(new Email(replyTo));
        }

        Personalization personalization = new Personalization();

        Map<String, Object> map = inputParameters.getMap(DYNAMIC_TEMPLATE, Object.class, Map.of());

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            personalization.addDynamicTemplateData(entry.getKey(), entry.getValue());
        }

        List<String> toList = inputParameters.getList(TO, String.class, List.of());

        for (String to : toList) {
            personalization.addTo(new Email(to));
        }

        List<String> ccList = inputParameters.getList(CC, String.class, List.of());

        for (String cc : ccList) {
            personalization.addCc(new Email(cc));
        }

        List<String> bccList = inputParameters.getList(BCC, String.class, List.of());

        for (String bcc : bccList) {
            personalization.addBcc(new Email(bcc));
        }

        mail.addPersonalization(personalization);

        List<Attachments> attachments = SendgridUtils.getAttachments(inputParameters, actionContext);

        for (Attachments attachment : attachments) {
            mail.addAttachments(attachment);
        }

        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        SendGrid sendGrid = new SendGrid(connectionParameters.getRequiredString(TOKEN));

        return sendGrid.api(request);
    }
}
