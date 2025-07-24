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

package com.bytechef.component.zendesk.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.BODY;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.COMMENT;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.PRIORITY;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.STATUS;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.SUBJECT;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TICKET;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TICKET_OBJECT_PROPERTY;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TYPE;
import static com.bytechef.component.zendesk.util.ZendeskUtils.checkIfNull;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskCreateTicketAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createTicket")
        .title("Create Ticket")
        .description("Creates a ticket.")
        .properties(
            string(SUBJECT)
                .label("Subject")
                .description("Subject of the ticket.")
                .required(false),
            string(TYPE)
                .label("Type")
                .description("Type of the ticket.")
                .options(
                    option("Problem", "problem"),
                    option("Incident", "incident"),
                    option("Question", "question"),
                    option("Task", "task"))
                .required(false),
            string(COMMENT)
                .label("Comment")
                .description("Comment of the ticket.")
                .required(true),
            string(PRIORITY)
                .label("Priority")
                .description("Priority of the ticket.")
                .options(
                    option("Low", "low"),
                    option("Normal", "normal"),
                    option("High", "high"),
                    option("Urgent", "urgent"))
                .required(false),
            string(STATUS)
                .label("Status")
                .description("Status of the ticket.")
                .options(
                    option("New", "new"),
                    option("Open", "open"),
                    option("Pending", "pending"),
                    option("Hold", "hold"),
                    option("Solved", "solved"),
                    option("Closed", "closed"))
                .required(false))
        .output(outputSchema(TICKET_OBJECT_PROPERTY))
        .perform(ZendeskCreateTicketAction::perform);

    private ZendeskCreateTicketAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Map<String, Object>> response = context.http(http -> http.post("/tickets"))
            .body(
                Body.of(
                    TICKET, Map.of(
                        COMMENT, Map.of(BODY, inputParameters.getRequiredString(COMMENT)),
                        PRIORITY, checkIfNull(inputParameters.getString(PRIORITY)),
                        SUBJECT, checkIfNull(inputParameters.getString(SUBJECT)),
                        TYPE, checkIfNull(inputParameters.getString(TYPE)),
                        STATUS, checkIfNull(inputParameters.getString(STATUS)))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get(TICKET);
    }
}
