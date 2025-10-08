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

package com.bytechef.component.bolna.action;

import static com.bytechef.component.bolna.constant.BolnaConstants.AGENT_ID;
import static com.bytechef.component.bolna.constant.BolnaConstants.RECIPIENT_PHONE_NUMBER;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.bolna.util.BolnaUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BolnaMakePhoneCallAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("makePhoneCall")
        .title("Make Phone Call")
        .description("Make a phone call using voice AI agent.")
        .properties(
            string(AGENT_ID)
                .label("Agent ID")
                .description("Agent id which will initiate the outbound call.")
                .options((OptionsFunction<String>) BolnaUtils::getAgentIdOptions)
                .required(true),
            string(RECIPIENT_PHONE_NUMBER)
                .label("Recipient Phone Number")
                .description("Phone number of the recipient alongwith country code (in E.164 format).")
                .options((OptionsFunction<String>) BolnaUtils::getPhoneNumbersOptions)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("message")
                            .description("Response message for the call initiated."),
                        string("status")
                            .description("Status of the call."),
                        string("execution_id")
                            .description("Unique execution id or call id identifier of the call."))))
        .perform(BolnaMakePhoneCallAction::perform);

    private BolnaMakePhoneCallAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(http -> http.post("/call"))
            .body(Http.Body.of(
                Map.of(
                    AGENT_ID, inputParameters.getRequiredString(AGENT_ID),
                    RECIPIENT_PHONE_NUMBER, inputParameters.getRequiredString(RECIPIENT_PHONE_NUMBER))))
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
