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

import static com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ATTACHMENTS;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.BASE_URL;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CC;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CONTENT_TYPE;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.EMAIL_SEND;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.FROM;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SUBJECT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TEXT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Krišković
 * @author Luka Ljubić
 */
public final class SendgridSendEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(EMAIL_SEND)
        .title("Send an email")
        .description("Sends an email.")
        .properties(
            string(FROM)
                .label("From:")
                .description("Email address from which you want to send.")
                .maxLength(320)
                .required(true),
            array(TO)
                .label("To:")
                .items(string().controlType(Property.ControlType.EMAIL))
                .description("Email addresses which you want to send to.")
                .required(true),
            array(CC)
                .label("CC:")
                .description("Email address which receives a copy.")
                .items(string().controlType(Property.ControlType.EMAIL))
                .maxItems(1000)
                .required(false)
                .advancedOption(true),
            string(SUBJECT)
                .label("Subject")
                .description("Subject of your email")
                .minLength(1)
                .maxLength(998)
                .required(true),
            string(TEXT)
                .label("Message Body")
                .description("This is the message you want to send")
                .minLength(1)
                .required(true),
            string(CONTENT_TYPE)
                .label("Message type")
                .description("Message type for your content")
                .options(
                    option("Plain text", "text/plain"),
                    option("HTML", "text/html"))
                .defaultValue("text/plain")
                .required(true),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments you want to include with the email.")
                .items(fileEntry())
                .required(false))
        .outputSchema(
            object()
                .properties())
        .perform(SendgridSendEmailAction::perform);

    private static final Base64.Encoder ENCODER = Base64.getEncoder();

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        List<FileEntry> attachmentFiles = inputParameters.getList(ATTACHMENTS, FileEntry.class);

        List<Map<String, Object>> allAttachments = getAllAttachments(attachmentFiles, context);

        List<Map<String, String>> toList = convertToEmailList(inputParameters.getRequiredList(TO, String.class));
        List<Map<String, String>> ccList = convertToEmailList(inputParameters.getList(CC, String.class, List.of()));

        Map<String, List<Map<String, String>>> itemMap = new HashMap<>();

        itemMap.put(TO, toList);

        List<Map<?, ?>> personalization = new ArrayList<>(List.of(itemMap));

        if (!ccList.isEmpty()) {
            itemMap.put(CC, ccList);
        }

        context.http(http -> http.post(BASE_URL + "/mail/send"))
            .body(
                Body.of(
                    "personalizations", personalization,
                    FROM, Map.of("email", inputParameters.getRequiredString(FROM)),
                    SUBJECT, inputParameters.getRequiredString(SUBJECT),
                    "content", List.of(
                        Map.of(
                            CONTENT_TYPE, inputParameters.getRequiredString(CONTENT_TYPE),
                            "value", inputParameters.getRequiredString(TEXT))),
                    ATTACHMENTS, allAttachments))
            .configuration(responseType(ResponseType.JSON))
            .execute();

        return null;
    }

    private static List<Map<String, Object>> getAllAttachments(List<FileEntry> attachmentFiles, ActionContext context) {
        List<Map<String, Object>> allAttachments = new ArrayList<>();

        for (FileEntry attachment : attachmentFiles) {
            String fileContent = context.file(file -> ENCODER.encodeToString(file.readAllBytes(attachment)));

            Map<String, Object> fileDetails = new HashMap<>();

            fileDetails.put("content", fileContent);
            fileDetails.put("filename", attachment.getName());
            fileDetails.put("type", attachment.getMimeType());

            allAttachments.add(fileDetails);
        }

        return allAttachments;
    }

    private static List<Map<String, String>> convertToEmailList(List<String> emailList) {
        return emailList.stream()
            .map(email -> Collections.singletonMap("email", email))
            .toList();
    }

    private SendgridSendEmailAction() {
    }
}
