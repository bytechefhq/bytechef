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

package com.bytechef.component.loops.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.loops.constant.LoopsConstants.EMAIL_MESSAGE_ID;
import static com.bytechef.component.loops.constant.LoopsConstants.EMAIL_MESSAGE_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LoopsGetEmailMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getEmailMessage")
        .title("Get Email Message")
        .description(
            "Get an email message, including its subject, preview text, LMX body and current content revision.")
        .properties(
            string(EMAIL_MESSAGE_ID)
                .label("Email Message ID")
                .description("The ID of the email message, for example the Email Message ID of a campaign.")
                .required(true))
        .output(outputSchema(EMAIL_MESSAGE_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/loops_v1#get-email-message")
        .perform(LoopsGetEmailMessageAction::perform);

    private LoopsGetEmailMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context
            .http(http -> http.get("/email-messages/" + inputParameters.getRequiredString(EMAIL_MESSAGE_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
