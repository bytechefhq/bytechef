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

package com.bytechef.component.hubspot.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDSL.authorization;
import static com.bytechef.component.definition.ComponentDSL.connection;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;
import java.util.List;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class HubspotConnection {
    public static final ComponentDSL.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.hubapi.com/")
        .authorizations(authorization(
            AuthorizationType.OAUTH2_AUTHORIZATION_CODE.toLowerCase(), AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> "https://app.hubspot.com/oauth/authorize")
                // needs to match the scopes perfectly
                .scopes((connection, context) -> List.of(
//                    "crm.dealsplits.read_write", "crm.lists.read",
//                    "crm.lists.write", "crm.objects.carts.read", "crm.objects.carts.write",
//                    "crm.objects.commercepayments.read", "crm.objects.companies.read", "crm.objects.companies.write",
                    "crm.objects.contacts.read", "crm.objects.contacts.write"
//                    , "crm.objects.custom.read",
//                    "crm.objects.custom.write", "crm.objects.deals.read", "crm.objects.deals.write",
//                    "crm.objects.feedback_submissions.read", "crm.objects.goals.read", "crm.objects.invoices.read",
//                    "crm.objects.leads.read", "crm.objects.leads.write", "crm.objects.line_items.read",
//                    "crm.objects.line_items.write", "crm.objects.marketing_events.read",
//                    "crm.objects.marketing_events.write", "crm.objects.orders.read", "crm.objects.orders.write",
//                    "crm.objects.owners.read", "crm.objects.partner-clients.read", "crm.objects.partner-clients.write",
//                    "crm.objects.quotes.read", "crm.objects.quotes.write", "crm.objects.subscriptions.read",
//                    "crm.objects.users.read", "crm.objects.users.write", "crm.pipelines.orders.read",
//                    "crm.pipelines.orders.write", "crm.schemas.carts.read", "crm.schemas.carts.write",
//                    "crm.schemas.commercepayments.read", "crm.schemas.companies.read", "crm.schemas.companies.write",
//                    "crm.schemas.contacts.read", "crm.schemas.contacts.write", "crm.schemas.custom.read",
//                    "crm.schemas.deals.read", "crm.schemas.deals.write", "crm.schemas.invoices.read",
//                    "crm.schemas.line_items.read", "crm.schemas.orders.read", "crm.schemas.orders.write",
//                    "crm.schemas.quotes.read", "crm.schemas.subscriptions.read", "actions", "automation", "files",
//                    "forms", "tickets"
                ))
                .tokenUrl((connectionParameters, context) -> "https://api.hubapi.com/oauth/v1/token"));

    private HubspotConnection() {
    }
}
