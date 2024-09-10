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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getMessageOutputProperty;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getSimpleMessage;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;

/**
 * @author Monika Kušter
 */
public class GoogleMailGetMailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getMail")
        .title("Get Mail")
        .description("Get an email from your Gmail account via Id")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getMessageIdOptions)
                .required(true),
            FORMAT_PROPERTY,
            METADATA_HEADERS_PROPERTY)
        .output(GoogleMailGetMailAction::getOutput)
        .perform(GoogleMailGetMailAction::perform);

    private GoogleMailGetMailAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {
        Gmail gmail = GoogleServices.getMail(connectionParameters);

        String format = inputParameters.getRequiredString(FORMAT);

        Message message = GoogleMailUtils.getMessage(inputParameters, gmail);

        if (format.equals(SIMPLE)) {
            return getSimpleMessage(message, actionContext, gmail);
        } else {
            return message;
        }
    }

    public static OutputResponse getOutput(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return new OutputResponse(getMessageOutputProperty(inputParameters.getRequiredString(FORMAT)));
    }
}
