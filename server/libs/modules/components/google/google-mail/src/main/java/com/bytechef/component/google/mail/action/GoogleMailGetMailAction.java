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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS_PROPERTY;
import static com.bytechef.component.google.mail.definition.Format.SIMPLE;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getSimpleMessage;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

/**
 * @author Monika Kušter
 */
public class GoogleMailGetMailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getMail")
        .title("Get Mail")
        .description("Gets the specified email message.")
        .help("", "https://docs.bytechef.io/reference/components/google-mail_v1#get-mail")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to retrieve.")
                .required(true),
            FORMAT_PROPERTY,
            METADATA_HEADERS_PROPERTY)
        .output(GoogleMailUtils::getMessageOutput)
        .perform(GoogleMailGetMailAction::perform);

    private GoogleMailGetMailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Format format = inputParameters.get(FORMAT, Format.class, SIMPLE);
        Gmail gmail = GoogleServices.getMail(connectionParameters);

        Message message = GoogleMailUtils.getMessage(inputParameters, gmail);

        if (format == SIMPLE) {
            return getSimpleMessage(message, context, gmail);
        } else {
            return message;
        }
    }
}
