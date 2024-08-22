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
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FULL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.GET_THREAD;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RAW;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleMailGetThreadAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_THREAD)
        .title("Get Thread")
        .description("Gets the specified thread.")
        .properties(
            string(ID)
                .label("Thread ID")
                .description("The ID of the thread to retrieve.")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getThreadIdOptions)
                .required(true),
            string(FORMAT)
                .label("Format")
                .description("The format to return the message in.")
                .options(
                    option("Minimal", MINIMAL,
                        "Returns only email message ID and labels; does not return the email headers, body, or payload."),
                    option("Full", FULL,
                        "Returns the full email message data with body content parsed in the payload field; the raw field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
                    option("Raw", RAW,
                        "Returns the full email message data with body content in the raw field as a base64url encoded string; the payload field is not used. Format cannot be used when accessing the api using the gmail.metadata scope."),
                    option("Metadata", "metadata", "Returns only email message ID, labels, and email headers."))
                .defaultValue(FULL)
                .required(false),
            METADATA_HEADERS_PROPERTY)
        .output(GoogleMailUtils.getOutput())
        .perform(GoogleMailGetThreadAction::perform);

    private GoogleMailGetThreadAction() {
    }

    public static Thread perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Gmail service = GoogleServices.getMail(connectionParameters);

        return service.users()
            .threads()
            .get("me", inputParameters.getRequiredString(ID))
            .setFormat(inputParameters.getString(FORMAT))
            .setMetadataHeaders(inputParameters.getList(METADATA_HEADERS, String.class, List.of()))
            .execute();
    }
}
