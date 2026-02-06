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

package com.bytechef.component.graphql.client;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.graphql.client.action.GraphQlClientQueryAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 **/
@AutoService(ComponentHandler.class)
public class GraphQlClientComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("graphQl")
        .title("GraphQL Client")
        .description(
            "Execute GraphQL queries against a GraphQL API, allowing you to fetch exactly the data you need " +
                "using queries and variables.")
        .icon("path:assets/graphql-client.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(GraphQlClientQueryAction.ACTION_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
