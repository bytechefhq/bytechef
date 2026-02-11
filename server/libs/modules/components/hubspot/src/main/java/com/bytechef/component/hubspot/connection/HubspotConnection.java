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

package com.bytechef.component.hubspot.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides the component connection definition.
 *
 * @generated
 */
public class HubspotConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://api.hubapi.com")
        .authorizations(authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
            .title("OAuth2 Authorization Code")
            .properties(
                string(CLIENT_ID)
                    .label("Client Id")
                    .required(true),
                string(CLIENT_SECRET)
                    .label("Client Secret")
                    .required(true))
            .authorizationUrl((connectionParameters, context) -> "https://app.hubspot.com/oauth/authorize")
            .scopes((connectionParameters, context) -> {
                Map<String, Boolean> scopeMap = new HashMap<>();

                scopeMap.put("account-info.security.read", false);
                scopeMap.put("accounting", false);
                scopeMap.put("actions", false);
                scopeMap.put("analytics.behavioral_events.send", false);
                scopeMap.put("automation", false);
                scopeMap.put("automation.sequences.enrollments.write", false);
                scopeMap.put("automation.sequences.read", false);
                scopeMap.put("behavioral_events.event_definitions.read_write", false);
                scopeMap.put("business-intelligence", false);
                scopeMap.put("business_units.view.read", false);
                scopeMap.put("cms.domains.read", false);
                scopeMap.put("cms.domains.write", false);
                scopeMap.put("cms.functions.read", false);
                scopeMap.put("cms.functions.write", false);
                scopeMap.put("cms.knowledge_base.articles.publish", false);
                scopeMap.put("cms.knowledge_base.articles.read", false);
                scopeMap.put("cms.knowledge_base.articles.write", false);
                scopeMap.put("cms.knowledge_base.settings.read", false);
                scopeMap.put("cms.knowledge_base.settings.write", false);
                scopeMap.put("cms.membership.access_groups.read", false);
                scopeMap.put("cms.membership.access_groups.write", false);
                scopeMap.put("collector.graphql_query.execute", false);
                scopeMap.put("collector.graphql_schema.read", false);
                scopeMap.put("communication_preferences.read", false);
                scopeMap.put("communication_preferences.read_write", false);
                scopeMap.put("communication_preferences.statuses.batch.read", false);
                scopeMap.put("communication_preferences.statuses.batch.write", false);
                scopeMap.put("communication_preferences.write", false);
                scopeMap.put("content", false);
                scopeMap.put("conversations.custom_channels.read", false);
                scopeMap.put("conversations.custom_channels.write", false);
                scopeMap.put("conversations.read", false);
                scopeMap.put("conversations.visitor_identification.tokens.create", false);
                scopeMap.put("conversations.write", false);
                scopeMap.put("crm.dealsplits.read_write", false);
                scopeMap.put("crm.export", false);
                scopeMap.put("crm.import", false);
                scopeMap.put("crm.lists.read", false);
                scopeMap.put("crm.lists.write", false);
                scopeMap.put("crm.objects.appointments.read", false);
                scopeMap.put("crm.objects.appointments.sensitive.read", false);
                scopeMap.put("crm.objects.appointments.sensitive.write", false);
                scopeMap.put("crm.objects.appointments.write", false);
                scopeMap.put("crm.objects.carts.read", false);
                scopeMap.put("crm.objects.carts.write", false);
                scopeMap.put("crm.objects.commercepayments.read", false);
                scopeMap.put("crm.objects.companies.highly_sensitive.read", false);
                scopeMap.put("crm.objects.companies.highly_sensitive.write", false);
                scopeMap.put("crm.objects.companies.read", false);
                scopeMap.put("crm.objects.companies.sensitive.read", false);
                scopeMap.put("crm.objects.companies.sensitive.write", false);
                scopeMap.put("crm.objects.companies.write", false);
                scopeMap.put("crm.objects.contacts.highly_sensitive.read", false);
                scopeMap.put("crm.objects.contacts.highly_sensitive.write", false);
                scopeMap.put("crm.objects.contacts.read", true);
                scopeMap.put("crm.objects.contacts.sensitive.read", false);
                scopeMap.put("crm.objects.contacts.sensitive.write", false);
                scopeMap.put("crm.objects.contacts.write", true);
                scopeMap.put("crm.objects.courses.read", false);
                scopeMap.put("crm.objects.courses.write", false);
                scopeMap.put("crm.objects.custom.highly_sensitive.read", false);
                scopeMap.put("crm.objects.custom.highly_sensitive.write", false);
                scopeMap.put("crm.objects.custom.read", false);
                scopeMap.put("crm.objects.custom.sensitive.read", false);
                scopeMap.put("crm.objects.custom.sensitive.write", false);
                scopeMap.put("crm.objects.custom.write", false);
                scopeMap.put("crm.objects.deals.highly_sensitive.read", false);
                scopeMap.put("crm.objects.deals.highly_sensitive.write", false);
                scopeMap.put("crm.objects.deals.read", true);
                scopeMap.put("crm.objects.deals.sensitive.read", false);
                scopeMap.put("crm.objects.deals.sensitive.write", false);
                scopeMap.put("crm.objects.deals.write", true);
                scopeMap.put("crm.objects.feedback_submission.read", false);
                scopeMap.put("crm.objects.goals.read", false);
                scopeMap.put("crm.objects.invoices.read", false);
                scopeMap.put("crm.objects.leads.read", false);
                scopeMap.put("crm.objects.leads.write", false);
                scopeMap.put("crm.objects.line_items.read", false);
                scopeMap.put("crm.objects.line_items.write", false);
                scopeMap.put("crm.objects.listings.read", false);
                scopeMap.put("crm.objects.listings.write", false);
                scopeMap.put("crm.objects.marketing_events.read", false);
                scopeMap.put("crm.objects.marketing_events.write", false);
                scopeMap.put("crm.objects.orders.read", false);
                scopeMap.put("crm.objects.orders.write", false);
                scopeMap.put("crm.objects.owners.read", true);
                scopeMap.put("crm.objects.partner-clients.read", false);
                scopeMap.put("crm.objects.partner-clients.write", false);
                scopeMap.put("crm.objects.partner-services.read", false);
                scopeMap.put("crm.objects.partner-services.write", false);
                scopeMap.put("crm.objects.projects.highly_sensitive.read", false);
                scopeMap.put("crm.objects.projects.highly_sensitive.write", false);
                scopeMap.put("crm.objects.projects.read", false);
                scopeMap.put("crm.objects.projects.sensitive.read", false);
                scopeMap.put("crm.objects.projects.sensitive.write", false);
                scopeMap.put("crm.objects.projects.write", false);
                scopeMap.put("crm.objects.quotes.read", false);
                scopeMap.put("crm.objects.quotes.write", false);
                scopeMap.put("crm.objects.services.read", false);
                scopeMap.put("crm.objects.services.write", false);
                scopeMap.put("crm.objects.subscriptions.read", false);
                scopeMap.put("crm.objects.users.read", false);
                scopeMap.put("crm.objects.users.write", false);
                scopeMap.put("crm.pipelines.orders.read", false);
                scopeMap.put("crm.pipelines.orders.write", false);
                scopeMap.put("crm.schemas.appointments.read", false);
                scopeMap.put("crm.schemas.appointments.write", false);
                scopeMap.put("crm.schemas.carts.read", false);
                scopeMap.put("crm.schemas.carts.write", false);
                scopeMap.put("crm.schemas.commercepayments.read", false);
                scopeMap.put("crm.schemas.companies.read", false);
                scopeMap.put("crm.schemas.companies.write", false);
                scopeMap.put("crm.schemas.contacts.read", false);
                scopeMap.put("crm.schemas.contacts.write", false);
                scopeMap.put("crm.schemas.courses.read", false);
                scopeMap.put("crm.schemas.courses.write", false);
                scopeMap.put("crm.schemas.custom.read", false);
                scopeMap.put("crm.schemas.deals.read", false);
                scopeMap.put("crm.schemas.deals.write", false);
                scopeMap.put("crm.schemas.invoices.read", false);
                scopeMap.put("crm.schemas.invoices.write", false);
                scopeMap.put("crm.schemas.line_items.read", false);
                scopeMap.put("crm.schemas.listings.read", false);
                scopeMap.put("crm.schemas.listings.write", false);
                scopeMap.put("crm.schemas.orders.read", false);
                scopeMap.put("crm.schemas.orders.write", false);
                scopeMap.put("crm.schemas.projects.read", false);
                scopeMap.put("crm.schemas.projects.write", false);
                scopeMap.put("crm.schemas.quotes.read", false);
                scopeMap.put("crm.schemas.services.read", false);
                scopeMap.put("crm.schemas.services.write", false);
                scopeMap.put("crm.schemas.subscriptions.read", false);
                scopeMap.put("crm.schemas.subscriptions.write", false);
                scopeMap.put("ctas.read", false);
                scopeMap.put("e-commerce", false);
                scopeMap.put("external_integrations.forms.access", false);
                scopeMap.put("files", false);
                scopeMap.put("files.ui_hidden.read", false);
                scopeMap.put("forms", false);
                scopeMap.put("forms-uploaded-files", false);
                scopeMap.put("hubdb", false);
                scopeMap.put("integration-sync", false);
                scopeMap.put("marketing-email", false);
                scopeMap.put("marketing.campaigns.read", false);
                scopeMap.put("marketing.campaigns.revenue.read", false);
                scopeMap.put("marketing.campaigns.write", false);
                scopeMap.put("media_bridge.read", false);
                scopeMap.put("media_bridge.write", false);
                scopeMap.put("oauth", false);
                scopeMap.put("sales-email-read", false);
                scopeMap.put("scheduler.meetings.meeting-link.read", false);
                scopeMap.put("settings.billing.write", false);
                scopeMap.put("settings.currencies.read", false);
                scopeMap.put("settings.currencies.write", false);
                scopeMap.put("settings.users.read", false);
                scopeMap.put("settings.users.team.write", false);
                scopeMap.put("settings.users.teams.read", false);
                scopeMap.put("settings.users.write", false);
                scopeMap.put("social", false);
                scopeMap.put("tax_rates.read", false);
                scopeMap.put("tickets", true);
                scopeMap.put("tickets.highly_sensitive", false);
                scopeMap.put("tickets.sensitive", false);
                scopeMap.put("timeline", false);
                scopeMap.put("transactional-email", false);

                return scopeMap;
            })
            .tokenUrl((connectionParameters, context) -> "https://api.hubapi.com/oauth/v1/token")
            .refreshUrl((connectionParameters, context) -> "https://api.hubapi.com/oauth/v1/token"));

    private HubspotConnection() {
    }
}
