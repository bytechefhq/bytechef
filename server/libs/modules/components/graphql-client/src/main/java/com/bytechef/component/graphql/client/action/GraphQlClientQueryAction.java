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

package com.bytechef.component.graphql.client.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.GRAPHQL_ENDPOINT;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.HEADERS;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.QUERY;
import static com.bytechef.component.graphql.client.constant.GraphQlConstants.VARIABLES;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Monika Kušter
 **/
public class GraphQlClientQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("rawQuery")
        .title("Raw Query")
        .description("Run a raw query to a GraphQL endpoint.")
        .help("", "https://docs.bytechef.io/reference/components/graphql-client_v1#raw-query")
        .properties(
            string(GRAPHQL_ENDPOINT)
                .label("GraphQL Endpoint")
                .description("The endpoint where GraphQL requests will be sent.")
                .controlType(ControlType.URL)
                .required(true),
            object(HEADERS)
                .label("Headers")
                .description("Headers that will be sent in the HTTP request alongside the GraphQL query.")
                .additionalProperties(string(), integer(), number(), bool())
                .required(false),
            object(VARIABLES)
                .label("Variables")
                .description("Variables that will be sent with the GraphQL query.")
                .additionalProperties(string(), integer(), number(), bool())
                .required(false),
            string(QUERY)
                .label("Query")
                .description("Query that will be sent to the GraphQL endpoint.")
                .controlType(ControlType.TEXT_AREA)
                .required(false))
        .output()
        .perform(GraphQlClientQueryAction::perform);

    private GraphQlClientQueryAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> headers = inputParameters.getMap(HEADERS, Object.class, Map.of());

        Map<String, List<String>> headerMap = headers.entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    Object value = entry.getValue();

                    return List.of(value.toString());
                }));

        return context.http(http -> http.post(inputParameters.getRequiredString(GRAPHQL_ENDPOINT)))
            .headers(headerMap)
            .body(
                Http.Body.of(
                    QUERY, inputParameters.getString(QUERY),
                    VARIABLES, inputParameters.getMap(VARIABLES, Object.class)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}
