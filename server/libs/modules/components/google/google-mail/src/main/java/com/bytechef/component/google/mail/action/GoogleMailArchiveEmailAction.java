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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import java.io.IOException;
import java.util.List;

/**
 * @author Ivona Pavela
 */
public class GoogleMailArchiveEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("archiveEmail")
        .title("Archive Email")
        .description("Archive an email message.")
        .help("", "https://docs.bytechef.io/reference/components/google-mail_v1#archive-email")
        .properties(
            string(ID)
                .label("Message ID")
                .description("ID of the message to be archived.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID)
                            .description("The ID of the message."),
                        string(THREAD_ID)
                            .description("The ID of the thread the message belongs to."),
                        array(LABEL_IDS)
                            .description("List of IDs of labels applied to this message.")
                            .items(string()))))
        .perform(GoogleMailArchiveEmailAction::perform);

    public static Message perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Gmail gmail = GoogleServices.getMail(connectionParameters);

        try {
            return gmail.users()
                .messages()
                .modify(
                    ME,
                    inputParameters.getRequiredString(ID),
                    new ModifyMessageRequest().setRemoveLabelIds(List.of("INBOX")))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
