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

package com.bytechef.component.sendgrid.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SEND_AT;
import static com.bytechef.component.sendgrid.constant.SendgridConstants.SINGLE_SEND_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SendgridScheduleSingleSendAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("scheduleSingleSend")
        .title("Schedule Single Send")
        .description("Schedules a Marketing Campaigns Single Send, or sends it immediately.")
        .properties(
            string(SINGLE_SEND_ID)
                .label("Single Send ID")
                .description("The ID of the Single Send, as returned by Create Single Send.")
                .required(true),
            string(SEND_AT)
                .label("Send At")
                .description(
                    "When to send the Single Send: a future ISO 8601 date-time such as 2026-12-01T09:00:00Z, " +
                        "or now to send it immediately.")
                .defaultValue("now")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("send_at")
                            .description("The time the Single Send is scheduled to be sent."),
                        string("status")
                            .description("The status of the Single Send, scheduled on success."))))
        .help("", "https://docs.bytechef.io/reference/components/sendgrid_v1#schedule-single-send")
        .perform(SendgridScheduleSingleSendAction::perform);

    private SendgridScheduleSingleSendAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String singleSendId = inputParameters.getRequiredString(SINGLE_SEND_ID);

        return context.http(http -> http.put("/marketing/singlesends/" + singleSendId + "/schedule"))
            .body(Body.of(Map.of("send_at", inputParameters.getRequiredString(SEND_AT))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
