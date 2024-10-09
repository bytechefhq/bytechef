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

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import java.io.IOException;

/**
 * @author J. Iamsamang
 */
public class GoogleMailAddLabelAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("addLabels")
        .title("Add labels")
        .description("Add labels to an email in your Gmail account")
        .properties(
            string(ID)
                .label("Message ID")
                .description("ID of the message to add labels")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getMessageIdOptions)
                .required(true),
            array(LABEL_IDS)
                .label("Labels")
                .description("Labels to add to this message. You can add up to 100 labels with each update.")
                .items(
                    string("label")
                        .label("Label")
                        .description("Label to add to this message")
                        .options((ActionOptionsFunction<String>) GoogleMailUtils::getLabelOptions))
                .maxItems(100)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(ID),
                        array(LABEL_IDS)
                            .items(string()),
                        string(THREAD_ID))))
        .perform(GoogleMailAddLabelAction::perform);

    private GoogleMailAddLabelAction() {
    }

    public static Message perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        ModifyMessageRequest messageRequest = new ModifyMessageRequest()
            .setAddLabelIds(inputParameters.getRequiredList(LABEL_IDS, String.class));

        return gmail
            .users()
            .messages()
            .modify(ME, inputParameters.getRequiredString(ID), messageRequest)
            .execute();
    }
}
