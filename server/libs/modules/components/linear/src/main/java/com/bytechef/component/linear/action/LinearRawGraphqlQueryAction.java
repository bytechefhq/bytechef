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

package com.bytechef.component.linear.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.linear.constant.LinearConstants.QUERY;
import static com.bytechef.component.linear.constant.LinearConstants.VARIABLES;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;

/**
 * @author Marija Horvat
 */
public class LinearRawGraphqlQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("rawGraphqlQuery")
        .title("Raw Graphql Query")
        .description("Perform a raw Graphql query.")
        .properties(
            string(QUERY)
                .label("Query")
                .description("The query to perform.")
                .required(true),
            string(VARIABLES)
                .label("Variables")
                .description("The variables for the query.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("data")
                            .description("Response data."))))
        .perform(LinearRawGraphqlQueryAction::perform);

    private LinearRawGraphqlQueryAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Object jsonVariables = null;
        String variables = inputParameters.getString(VARIABLES);

        if (variables != null) {
            jsonVariables = context.json(json -> json.read(variables));
        }

        return context
            .http(http -> http.post("/graphql"))
            .body(Body.of(QUERY, inputParameters.getRequiredString(QUERY), VARIABLES, jsonVariables))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
