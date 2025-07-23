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

package com.bytechef.component.vbout.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.vbout.constant.VboutConstants.CONFIRMATION_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.CONFIRMATION_MESSAGE;
import static com.bytechef.component.vbout.constant.VboutConstants.EMAIL_SUBJECT;
import static com.bytechef.component.vbout.constant.VboutConstants.ERROR_MESSAGE;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.FROM_NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.NAME;
import static com.bytechef.component.vbout.constant.VboutConstants.NOTIFY_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.REPLY_TO;
import static com.bytechef.component.vbout.constant.VboutConstants.SUCCESS_EMAIL;
import static com.bytechef.component.vbout.constant.VboutConstants.SUCCESS_MESSAGE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class VboutCreateEmailListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createEmailList")
        .title("Create Email List")
        .description("Creates a new email list.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the list.")
                .required(true),
            string(EMAIL_SUBJECT)
                .label("Email Subject")
                .description("The default subscription subject.")
                .required(false),
            string(REPLY_TO)
                .label("Reply To")
                .description("The reply to email of the list.")
                .required(false),
            string(FROM_EMAIL)
                .label("From Mail")
                .description("The from email of the list.")
                .required(false),
            string(FROM_NAME)
                .label("From Name")
                .description("The from name of the list.")
                .required(false),
            string(NOTIFY_EMAIL)
                .label("Notify Email")
                .description("Notification email.")
                .required(false),
            string(SUCCESS_EMAIL)
                .label("Success Email")
                .description("Subscription success email.")
                .required(false),
            string(SUCCESS_MESSAGE)
                .label("Success Message")
                .description("Subscription success message.")
                .required(false),
            string(ERROR_MESSAGE)
                .label("Error Message")
                .description("Subscription error message.")
                .required(false),
            string(CONFIRMATION_EMAIL)
                .label("Confirmation Email")
                .description("Confirmation email.")
                .required(false),
            string(CONFIRMATION_MESSAGE)
                .label("Confirmation Message")
                .description("Confirmation message.")
                .required(false))
        .perform(VboutCreateEmailListAction::perform);

    private VboutCreateEmailListAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        context
            .http(http -> http.post("/emailMarketing/AddList"))
            .configuration(responseType(ResponseType.JSON))
            .queryParameters(
                NAME, inputParameters.getRequiredString(NAME),
                EMAIL_SUBJECT, inputParameters.getString(EMAIL_SUBJECT),
                REPLY_TO, inputParameters.getString(REPLY_TO),
                FROM_EMAIL, inputParameters.getString(FROM_EMAIL),
                FROM_NAME, inputParameters.getString(FROM_NAME),
                NOTIFY_EMAIL, inputParameters.getString(NOTIFY_EMAIL),
                SUCCESS_EMAIL, inputParameters.getString(SUCCESS_EMAIL),
                SUCCESS_MESSAGE, inputParameters.getString(SUCCESS_MESSAGE),
                ERROR_MESSAGE, inputParameters.getString(ERROR_MESSAGE),
                CONFIRMATION_EMAIL, inputParameters.getString(CONFIRMATION_EMAIL),
                CONFIRMATION_MESSAGE, inputParameters.getString(CONFIRMATION_MESSAGE))
            .execute();

        return null;
    }
}
