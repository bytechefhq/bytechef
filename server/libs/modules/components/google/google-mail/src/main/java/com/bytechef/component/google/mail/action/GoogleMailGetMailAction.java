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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_MAIL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_MAIL_DESCRIPTION;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_MAIL_TITLE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS_PROPERTY;
import static com.bytechef.component.google.mail.definition.Format.SIMPLE;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getSimpleMessage;

import com.bytechef.component.definition.ActionDefinition.OutputFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleMailGetMailAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(ID)
            .label("Message ID")
            .description("The ID of the message to retrieve.")
            .options((ActionOptionsFunction<String>) GoogleMailUtils::getMessageIdOptions)
            .required(true),
        FORMAT_PROPERTY,
        METADATA_HEADERS_PROPERTY
    };

    public static final OutputFunction OUTPUT_FUNCTION = GoogleMailUtils::getMessageOutput;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_MAIL)
        .title(GET_MAIL_TITLE)
        .description(GET_MAIL_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_FUNCTION)
        .perform(GoogleMailGetMailAction::perform);

    private GoogleMailGetMailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context)
        throws IOException {

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
