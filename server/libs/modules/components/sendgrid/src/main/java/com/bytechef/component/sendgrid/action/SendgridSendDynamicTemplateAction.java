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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.ATTACHMENTS;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.CC;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.DYNAMIC_TEMPLATE_DATA;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.FROM;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TEMPLATE_ID;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.TO;
import static com.bytechef.component.sendgrid.util.SendgridUtils.convertToEmailList;
import static com.bytechef.component.sendgrid.util.SendgridUtils.getAllAttachments;
import static com.bytechef.component.sendgrid.util.SendgridUtils.sendEmail;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.sendgrid.util.SendgridUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class SendgridSendDynamicTemplateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendDynamicTemplate")
        .title("Send Dynamic Template")
        .description("Send an email using a dynamic template.")
        .properties(
            string(FROM)
                .label("From")
                .description("Email address from which you want to send.")
                .maxLength(320)
                .required(true),
            array(TO)
                .label("To")
                .items(string().controlType(Property.ControlType.EMAIL))
                .description("Email addresses which you want to send to.")
                .required(true),
            array(CC)
                .label("CC")
                .description("Email address which receives a copy.")
                .items(string().controlType(Property.ControlType.EMAIL))
                .maxItems(1000)
                .required(false)
                .advancedOption(true),
            string(TEMPLATE_ID)
                .label("Template ID")
                .description("Dynamic template ID.")
                .options((OptionsFunction<String>) SendgridUtils::getTemplateIdOptions)
                .required(true),
            object(DYNAMIC_TEMPLATE_DATA)
                .label("Dynamic Template Data")
                .description("Data passed to the SendGrid dynamic template.")
                .required(false),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("A list of attachments you want to include with the email.")
                .items(fileEntry())
                .required(false))
        .help("", "https://docs.bytechef.io/reference/components/sendgrid_v1#send-dynamic-template")
        .perform(SendgridSendDynamicTemplateAction::perform);

    private SendgridSendDynamicTemplateAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<FileEntry> attachmentFiles = inputParameters.getList(ATTACHMENTS, FileEntry.class);

        List<Map<String, Object>> allAttachments = getAllAttachments(attachmentFiles, context);

        List<Map<String, String>> toList = convertToEmailList(inputParameters.getRequiredList(TO, String.class));
        List<Map<String, String>> ccList = convertToEmailList(inputParameters.getList(CC, String.class, List.of()));

        Map<String, Object> itemMap = new HashMap<>();

        itemMap.put(TO, toList);

        if (!ccList.isEmpty()) {
            itemMap.put(CC, ccList);
        }

        itemMap.put(DYNAMIC_TEMPLATE_DATA, inputParameters.getMap(DYNAMIC_TEMPLATE_DATA, Map.of()));

        List<Map<?, ?>> personalization = new ArrayList<>(List.of(itemMap));

        Map<String, Object> body = new HashMap<>();

        body.put("personalizations", personalization);
        body.put(FROM, Map.of("email", inputParameters.getRequiredString(FROM)));

        body.put("template_id", inputParameters.getRequiredString(TEMPLATE_ID));

        if (!allAttachments.isEmpty()) {
            body.put(ATTACHMENTS, allAttachments);
        }

        return sendEmail(context, body);
    }
}
