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

package com.bytechef.component.affinity.action;

import static com.bytechef.component.affinity.constant.AffinityConstants.BASE_URL;
import static com.bytechef.component.affinity.constant.AffinityConstants.CREATE_PERSON;
import static com.bytechef.component.affinity.constant.AffinityConstants.EMAILS;
import static com.bytechef.component.affinity.constant.AffinityConstants.FIRST_NAME;
import static com.bytechef.component.affinity.constant.AffinityConstants.LAST_NAME;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Domiter
 */
public class AffinityCreatePersonAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_PERSON)
        .title("Create person")
        .description("Creates a new person")
        .properties(
            string(FIRST_NAME)
                .label("First name")
                .description("The first name of the person.")
                .required(true),
            string(LAST_NAME)
                .label("Last name")
                .description("The last name of the person.")
                .required(true),
            array(EMAILS)
                .label("Emails")
                .description("The email addresses of the person.")
                .items(string())
                .required(false))
        .outputSchema(
            object()
                .properties(
                    integer("id"),
                    string(FIRST_NAME),
                    string(LAST_NAME),
                    array(EMAILS)
                        .items(string())))
        .perform(AffinityCreatePersonAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_PERSONS_CONTEXT_FUNCTION =
        http -> http.post(BASE_URL + "persons");

    private AffinityCreatePersonAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_PERSONS_CONTEXT_FUNCTION)
            .body(
                Http.Body.of(
                    FIRST_NAME, inputParameters.getRequiredString(FIRST_NAME),
                    LAST_NAME, inputParameters.getRequiredString(LAST_NAME),
                    EMAILS, inputParameters.getList(EMAILS, String.class)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
