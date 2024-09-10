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

import static com.bytechef.component.affinity.constant.AffinityConstants.CREATE_OPPORTUNITY;
import static com.bytechef.component.affinity.constant.AffinityConstants.NAME;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class AffinityCreateOpportunityAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CREATE_OPPORTUNITY)
        .title("Create opportunity")
        .description("Creates a new opportunity")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the opportunity.")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("id"),
                        string(NAME))))
        .perform(AffinityCreateOpportunityAction::perform);

    protected static final ContextFunction<Http, Http.Executor> POST_OPPORTUNITIES_CONTEXT_FUNCTION =
        http -> http.post("/opportunities");

    private AffinityCreateOpportunityAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return actionContext.http(POST_OPPORTUNITIES_CONTEXT_FUNCTION)
            .body(Http.Body.of(Map.of(NAME, inputParameters.getRequiredString(NAME))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
