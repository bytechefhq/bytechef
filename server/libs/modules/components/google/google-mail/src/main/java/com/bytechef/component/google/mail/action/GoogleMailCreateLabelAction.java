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

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;

import java.io.IOException;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NAME;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

/**
 * @author Ivona Pavela
 */
public class GoogleMailCreateLabelAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createLabel")
        .title("Create Label")
        .description("Creates a new label.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The display name of the newly created label.")
                .minLength(1)
                .maxLength(255)
                .required(true))
        .output(
            outputSchema(
                string()))
        .perform(GoogleMailCreateLabelAction::perform);

    public static Label perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        Gmail gmail = GoogleServices.getMail(connectionParameters);

        Label label = new Label()
            .setName(inputParameters.getRequiredString(NAME))
            .setLabelListVisibility("labelShow")
            .setMessageListVisibility("show");

        try {
            return gmail.users()
                .labels()
                .create(ME, label)
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
