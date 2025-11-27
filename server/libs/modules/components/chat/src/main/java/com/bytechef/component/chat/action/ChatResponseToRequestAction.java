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

package com.bytechef.component.chat.action;

import static com.bytechef.component.chat.constant.ChatConstants.ATTACHMENTS;
import static com.bytechef.component.chat.constant.ChatConstants.MESSAGE;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.WebhookResponse;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class ChatResponseToRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("responseToRequest")
        .title("Response to Chat Request")
        .description("Converts the response to chat request.")
        .properties(
            string(MESSAGE)
                .label("Message")
                .description("The message of the response."),
            array(ATTACHMENTS)
                .label("Attachments")
                .description("The attachments of the response.")
                .placeholder("Add attachment")
                .items(fileEntry()))
        .output(ChatResponseToRequestAction::output)
        .perform(ChatResponseToRequestAction::perform);

    protected static OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return OutputResponse.of(
            Map.of(
                MESSAGE, inputParameters.getString(MESSAGE, ""),
                ATTACHMENTS, inputParameters.getFileEntries(ATTACHMENTS, List.of())));
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return WebhookResponse.json(
            Map.of(
                MESSAGE, inputParameters.getString(MESSAGE, ""),
                ATTACHMENTS, inputParameters.getFileEntries(ATTACHMENTS, List.of())));
    }
}
