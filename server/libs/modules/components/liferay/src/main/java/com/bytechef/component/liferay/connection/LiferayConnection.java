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

package com.bytechef.component.liferay.connection;

import static com.bytechef.component.definition.Authorization.ADD_TO;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.KEY;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ConnectionDefinition.BASE_URI;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.ApiTokenLocation;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Igor Beslic
 */
public class LiferayConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(BASE_URI)
                .label("Base URI")
                .required(true)
                .description("If set, it will be combined Liferay API Endpoint attribute value."))
        .authorizationRequired(true)
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2")
                .properties(
                    string(CLIENT_ID)
                        .label("Client ID")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connectionParameters, context) -> connectionParameters.getRequiredString(BASE_URI)
                    + "o/oauth2/authorize")
                .tokenUrl((connectionParameters, context) -> connectionParameters.getRequiredString(BASE_URI)
                    + "o/oauth2/token")
                .scopes((connectionParameters, context) -> {
                    Map<String, Boolean> map = new LinkedHashMap<>();

                    map.put("Liferay.Headless.Discovery.OpenAPI.everything.read", true);
                    map.put("Liferay.Analytics.Reports.REST.everything.read", false);
                    map.put("Liferay.Analytyics.Settings.REST.everything.read", false);
                    map.put("Liferay.Analytyics.Settings.REST.everything.write", false);
                    map.put("Liferay.Analytyics.Settings.REST.everything", false);
                    map.put("Liferay.Batch.Planner.REST.everything.read", false);
                    map.put("Liferay.Batch.Planner.REST.everything", false);
                    map.put("Liferay.Batch.Planner.REST.everything.write", false);
                    map.put("Liferay.Bulk.REST.everything.write", false);
                    map.put("Liferay.Bulk.REST.everything.read", false);
                    map.put("Liferay.Bulk.REST.everything", false);
                    map.put("Liferay.Captcha.REST.everything.read", false);
                    map.put("Liferay.Captcha.REST.everything.write", false);
                    map.put("Liferay.Captcha.REST.everything", false);
                    map.put("Liferay.Change.Tracking.REST.everything.write", false);
                    map.put("Liferay.Change.Tracking.REST.everything.read", false);
                    map.put("Liferay.Change.Tracking.REST.everything", false);
                    map.put("Liferay.Commerce.COMMERCE_DEFAULT", false);
                    map.put("Liferay.Data.Engine.REST.everything.write", false);
                    map.put("Liferay.Data.Engine.REST.everything.read", false);
                    map.put("Liferay.Data.Engine.REST.everything", false);
                    map.put("Liferay.Digital.Signature.REST.everything.write", false);
                    map.put("Liferay.Digital.Signature.REST.everything.read", false);
                    map.put("Liferay.Digital.Signature.REST.everything", false);
                    map.put("Liferay.Dispatch.REST.everything.read", false);
                    map.put("Liferay.Dispatch.REST.everything.write", false);
                    map.put("Liferay.Dispatch.REST.everything", false);
                    map.put("Liferay.Headless.Admin.Address.everything.write", false);
                    map.put("Liferay.Headless.Admin.Address.everything.read", false);
                    map.put("Liferay.Headless.Admin.Address.everything", false);
                    map.put("Liferay.Headless.Admin.Content.everything", false);
                    map.put("Liferay.Headless.Admin.Content.everything.read", false);
                    map.put("Liferay.Headless.Admin.Content.everything.write", false);
                    map.put("Liferay.Headless.Admin.List.Type.everything", false);
                    map.put("Liferay.Headless.Admin.List.Type.everything.read", false);
                    map.put("Liferay.Headless.Admin.List.Type.everything.write", false);
                    map.put("Liferay.Headless.Admin.Site.everything", false);
                    map.put("Liferay.Headless.Admin.Site.everything.read", false);
                    map.put("Liferay.Headless.Admin.Site.everything.write", false);
                    map.put("Liferay.Headless.Admin.Taxonomy.everything.write", false);
                    map.put("Liferay.Headless.Admin.Taxonomy.everything.read", false);
                    map.put("Liferay.Headless.Admin.Taxonomy.everything", false);
                    map.put("Liferay.Headless.Admin.User.everything.read", false);
                    map.put("Liferay.Headless.Admin.User.everything.write", false);
                    map.put("Liferay.Headless.Admin.User.everything", false);
                    map.put("Liferay.Headless.Admin.Workflow.everything.read", false);
                    map.put("Liferay.Headless.Admin.Workflow.everything.write", false);
                    map.put("Liferay.Headless.Admin.Workflow.everything", false);
                    map.put("Liferay.Headless.Batch.Engine.everything.read", false);
                    map.put("Liferay.Headless.Batch.Engine.everything.write", false);
                    map.put("Liferay.Headless.Batch.Engine.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Account.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Account.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Account.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Catalog.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Catalog.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Catalog.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Channel.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Channel.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Channel.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Inventory.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Inventory.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Inventory.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Order.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Order.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Order.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Payment.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Payment.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Payment.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Pricing.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Pricing.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Pricing.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Shipment.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Shipment.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Admin.Shipment.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Site.Setting.everything", false);
                    map.put("Liferay.Headless.Commerce.Admin.Site.Setting.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Admin.Site.Setting.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Delivery.Catalog.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Delivery.Catalog.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Delivery.Catalog.everything", false);
                    map.put("Liferay.Headless.Commerce.Delivery.Order.everything.write", false);
                    map.put("Liferay.Headless.Commerce.Delivery.Order.everything.read", false);
                    map.put("Liferay.Headless.Commerce.Delivery.Order.everything", false);
                    map.put("Liferay.Headless.Commerce.Machine.Learning.everything.read", false);
                    map.put("Liferay.Headless.Delivery.everything.read", false);
                    map.put("Liferay.Headless.Delivery.everything.write", false);
                    map.put("Liferay.Headless.Delivery.everything", false);
                    map.put("Liferay.Headless.Discovery.API.everything.read", false);
                    map.put("Liferay.Headless.Form.everything.read", false);
                    map.put("Liferay.Headless.Form.everything.write", false);
                    map.put("Liferay.Headless.Form.everything", false);
                    map.put("Liferay.Headless.Portal.Instances.everything.write", false);
                    map.put("Liferay.Headless.Portal.Instances.everything.read", false);
                    map.put("Liferay.Headless.Portal.Instances.everything", false);
                    map.put("Liferay.Headless.Site.everything.read", false);
                    map.put("Liferay.Headless.Site.everything.write", false);
                    map.put("Liferay.Headless.Site.everything", false);
                    map.put("Liferay.Headless.User.Notification.everything", false);
                    map.put("Liferay.Headless.User.Notification.everything.read", false);
                    map.put("Liferay.Headless.User.Notification.everything.write", false);
                    map.put("Liferay.Notification.REST.everything.read", false);
                    map.put("Liferay.Notification.REST.everything.write", false);
                    map.put("Liferay.Notification.REST.everything", false);
                    map.put("Liferay.Object.Admin.REST.everything.read", false);
                    map.put("Liferay.Object.Admin.REST.everything.write", false);
                    map.put("Liferay.Object.Admin.REST.everything", false);
                    map.put("Liferay.Portal.Language.REST.everything", false);
                    map.put("Liferay.Portal.Language.REST.everything.write", false);
                    map.put("Liferay.Portal.Language.REST.everything.read", false);
                    map.put("Liferay.Portal.Search.REST.everything.read", false);
                    map.put("Liferay.Portal.Search.REST.everything.write", false);
                    map.put("Liferay.Portal.Search.REST.everything", false);
                    map.put("Liferay.Portal.Workflow.Metrics.REST.everything.write", false);
                    map.put("Liferay.Portal.Workflow.Metrics.REST.everything.read", false);
                    map.put("Liferay.Portal.Workflow.Metrics.REST.everything", false);
                    map.put("Liferay.Saml.Admin.REST.everything.write", false);
                    map.put("Liferay.Saml.Admin.REST.everything.read", false);
                    map.put("Liferay.Saml.Admin.REST.everything", false);
                    map.put("Liferay.Scim.REST.everything.read", false);
                    map.put("Liferay.Scim.REST.everything.write", false);
                    map.put("Liferay.Scim.REST.everything", false);
                    map.put("Liferay.Search.Experiences.REST.everything.read", false);
                    map.put("Liferay.Search.Experiences.REST.everything.write", false);
                    map.put("Liferay.Search.Experiences.REST.everything", false);
                    map.put("Liferay.Segments.Asah.REST.everything", false);
                    map.put("Liferay.Segments.Asah.REST.everything.read", false);
                    map.put("Liferay.Segments.Asah.REST.everything.write", false);

                    return map;
                }),
            authorization(AuthorizationType.API_KEY)
                .title("API Key")
                .properties(
                    string(KEY)
                        .label("Key")
                        .required(true)
                        .defaultValue(Authorization.API_TOKEN),
                    string(VALUE)
                        .label("Value")
                        .required(true),
                    string(ADD_TO)
                        .label("Add to")
                        .required(true)
                        .options(
                            option("Header", ApiTokenLocation.HEADER.name()),
                            option("QueryParams", ApiTokenLocation.QUERY_PARAMETERS.name()))),
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)))
        .help("", "https://docs.bytechef.io/reference/components/liferay_v1#connection-setup")
        .version(1);
}
