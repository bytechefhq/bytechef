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
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import java.io.IOException;

/**
 * @author J. Iamsamang
 */
public class GoogleMailDeleteMailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("deleteMail")
        .title("Delete Mail")
        .description("Delete an email from your Gmail account permanently via Id")
        .properties(
            string(ID)
                .label("Message ID")
                .description("The ID of the message to delete.")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getMessageIdOptions)
                .required(true))
        .perform(GoogleMailDeleteMailAction::perform);

    private GoogleMailDeleteMailAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Gmail gmail = GoogleServices.getMail(connectionParameters);

        try {
            return gmail
                .users()
                .messages()
                .delete(ME, inputParameters.getRequiredString(ID))
                .execute();
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}
