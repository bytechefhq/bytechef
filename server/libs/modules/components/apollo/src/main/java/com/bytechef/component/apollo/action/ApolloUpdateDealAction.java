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

package com.bytechef.component.apollo.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.apollo.util.ApolloUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ApolloUpdateDealAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("updateDeal")
        .title("Update Deal")
        .description("Updates the details of existing deals within your team's Apollo account.")
        .metadata(
            Map.of(
                "method", "PATCH",
                "path", "/opportunities"

            ))
        .properties(string("opportunity_id").label("Opportunity Id")
            .description("The ID for the deal you want to update.")
            .required(true)
            .options((ActionDefinition.OptionsFunction<String>) ApolloUtils::getOpportunityIdOptions)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            string("owner_id").label("Owner ID")
                .description("The ID for the deal owner within your team's Apollo account.")
                .required(false)
                .options((ActionDefinition.OptionsFunction<String>) ApolloUtils::getOwnerIdOptions)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("name").label("Name")
                .description("New name for the deal.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            date("closed_date").label("Close Date")
                .description("Updated estimated close date for the deal. This can be a future or past date.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("account_id").label("Account ID")
                .description(
                    "The ID for the account within your Apollo instance. This is the company that you are targeting as part of the deal being created.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .output(outputSchema(object()
            .properties(object("opportunity").properties(string("id").description("The ID of the deal.")
                .required(false),
                string("team_id").description("The ID of the team within your Apollo account.")
                    .required(false),
                string("owner_id").description("The ID of the deal owner within your team's Apollo account.")
                    .required(false),
                number("amount").description("The monetary value of the deal.")
                    .required(false),
                date("closed_date").description("The estimated close date for the deal.")
                    .required(false),
                string("account_id").description("The ID of the account associated with the deal.")
                    .required(false),
                string("description").description("The description of the deal.")
                    .required(false),
                string("name").description("The name of the deal.")
                    .required(false),
                object("currency").properties(string("name").description("The name of the currency.")
                    .required(false),
                    string("iso_code").description("The ISO code for the currency.")
                        .required(false),
                    string("symbol").description("The symbol for the currency.")
                        .required(false))
                    .required(false))
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ApolloUpdateDealAction() {
    }
}
