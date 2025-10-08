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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.BODY;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.COMMENT;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TICKET;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TICKET_ID;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.TICKET_OBJECT_PROPERTY;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.zendesk.util.ZendeskUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskCommentTicketAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("commentTicket")
        .title("Add Comment to Ticket")
        .description("Adds a comment to an existing ticket.")
        .properties(
            integer(TICKET_ID)
                .label("Ticket ID")
                .description("ID of the ticket that will get the comment.")
                .required(true)
                .options((OptionsFunction<Long>) ZendeskUtils::getTicketIdOptions),
            string(COMMENT)
                .label("Comment")
                .description("A ticket comment.")
                .required(true))
        .output(outputSchema(TICKET_OBJECT_PROPERTY))
        .perform(ZendeskCommentTicketAction::perform);

    private ZendeskCommentTicketAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Map<String, Object>> response = context
            .http(http -> http.put("/tickets/%s".formatted(inputParameters.getRequiredInteger(TICKET_ID))))
            .body(Body.of(TICKET, Map.of(COMMENT, Map.of(BODY, inputParameters.getRequiredString(COMMENT)))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get(TICKET);
    }
}
