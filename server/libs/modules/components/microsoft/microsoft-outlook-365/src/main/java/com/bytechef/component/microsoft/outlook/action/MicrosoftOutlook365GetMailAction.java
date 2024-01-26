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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.GET_MAIL;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.MESSAGE_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.requests.GraphServiceClient;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365GetMailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(GET_MAIL)
        .title("Get Mail")
        .description("Get a specific message")
        .properties(
            string(ID)
                .label("Message id")
                .description("Id of the message")
                .required(true))
        .outputSchema(MESSAGE_OUTPUT_PROPERTY)
        .perform(MicrosoftOutlook365GetMailAction::perform);

    private MicrosoftOutlook365GetMailAction() {
    }

    public static Message perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        GraphServiceClient<?> graphClient = MicrosoftOutlook365Utils.getGraphServiceClient();

        return graphClient.me()
            .messages(inputParameters.getRequiredString(ID))
            .buildRequest()
            .get();
    }
}
