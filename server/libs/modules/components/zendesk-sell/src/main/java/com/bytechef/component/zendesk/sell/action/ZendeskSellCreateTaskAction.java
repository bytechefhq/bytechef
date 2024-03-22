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

package com.bytechef.component.zendesk.sell.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.BASE_URL;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.CONTENT;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.CREATE_TASK;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.DATA;
import static com.bytechef.component.zendesk.sell.constant.ZendeskSellConstants.DUE_DATE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class ZendeskSellCreateTaskAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_TASK)
        .title("Create task")
        .description("Creates new Task")
        .properties(
            string(CONTENT)
                .label("Task name")
                .required(true),
            date(DUE_DATE)
                .label("Due Date")
                .required(false))
        .outputSchema(
            object()
                .properties(
                    object(DATA)
                        .properties(
                            integer("id"),
                            string(CONTENT),
                            date(DUE_DATE)),
                    object("meta")
                        .properties(
                            string("type"))))
        .perform(ZendeskSellCreateTaskAction::perform);

    private ZendeskSellCreateTaskAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(http -> http.post(BASE_URL + "/tasks"))
            .body(
                Http.Body.of(
                    "data",
                    new Object[] {
                        CONTENT, inputParameters.getRequiredString(CONTENT),
                        DUE_DATE, inputParameters.getDate(DUE_DATE)
                    }))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
