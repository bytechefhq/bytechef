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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.COMMENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365ReplyToEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replyToEmail")
        .title("Reply to Email")
        .description("Creates a new reply to email.")
        .properties(
            string(ID)
                .label("Message ID")
                .description("Id of the message to reply to.")
                .options((ActionOptionsFunction<String>) MicrosoftOutlook365OptionUtils::getMessageIdOptions)
                .required(true),
            string(COMMENT)
                .label("Comment")
                .description("Content of the reply to the email.")
                .required(true))
        .perform(MicrosoftOutlook365ReplyToEmailAction::perform);

    private MicrosoftOutlook365ReplyToEmailAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        actionContext.http(http -> http.post("/messages/" + inputParameters.getRequiredString(ID) + "/reply"))
            .body(
                Http.Body.of(
                    Map.of(COMMENT, inputParameters.getString(COMMENT))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute();

        return null;
    }
}
