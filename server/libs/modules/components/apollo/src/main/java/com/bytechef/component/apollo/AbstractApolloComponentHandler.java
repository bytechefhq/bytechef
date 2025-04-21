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

package com.bytechef.component.apollo;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.apollo.action.ApolloCreateDealAction;
import com.bytechef.component.apollo.action.ApolloEnrichCompanyAction;
import com.bytechef.component.apollo.action.ApolloEnrichPersonAction;
import com.bytechef.component.apollo.action.ApolloUpdateDealAction;
import com.bytechef.component.apollo.connection.ApolloConnection;
import com.bytechef.component.definition.ComponentDefinition;

/**
 * Provides the base implementation for the REST based component.
 *
 * @generated
 */
public abstract class AbstractApolloComponentHandler implements OpenApiComponentHandler {
    private final ComponentDefinition componentDefinition = modifyComponent(
        component("apollo")
            .title("Apollo")
            .description(
                "Apollo.io is a sales intelligence and engagement platform that provides tools for prospecting, lead generation, and sales automation to help businesses improve their sales processes and outreach efforts."))
                    .actions(modifyActions(ApolloUpdateDealAction.ACTION_DEFINITION,
                        ApolloCreateDealAction.ACTION_DEFINITION, ApolloEnrichPersonAction.ACTION_DEFINITION,
                        ApolloEnrichCompanyAction.ACTION_DEFINITION))
                    .connection(modifyConnection(ApolloConnection.CONNECTION_DEFINITION))
                    .clusterElements(modifyClusterElements(tool(ApolloUpdateDealAction.ACTION_DEFINITION),
                        tool(ApolloCreateDealAction.ACTION_DEFINITION),
                        tool(ApolloEnrichPersonAction.ACTION_DEFINITION),
                        tool(ApolloEnrichCompanyAction.ACTION_DEFINITION)))
                    .triggers(getTriggers());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
