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

package com.bytechef.component.zendesk.connection;

import static com.bytechef.component.definition.Authorization.API_TOKEN;
import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.EMAIL;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.SUBDOMAIN;

import com.bytechef.component.definition.Authorization.ApplyResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://%s.zendesk.com/api/v2".formatted(
            connectionParameters.getRequiredString(SUBDOMAIN)))
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .properties(
                    string(API_TOKEN)
                        .label("API Token")
                        .description(
                            "API tokens are managed in the Admin Center interface at Apps and integrations > APIs > " +
                                "API Tokens.")
                        .required(true),
                    string(EMAIL)
                        .label("Email")
                        .description("Your Zendesk account email.")
                        .required(true),
                    string(SUBDOMAIN)
                        .label("Subdomain")
                        .description("Subdomain of your Zendesk account (e.g. https://SUBDOMAIN.zendesk.com).")
                        .required(true))
                .apply((connectionParameters, context) -> {
                    String token = "%s/token:%s".formatted(
                        connectionParameters.getRequiredString(EMAIL),
                        connectionParameters.getRequiredString(API_TOKEN));

                    String base64EncodedToken = context.encoder(
                        encoder -> encoder.base64Encode(token.getBytes(StandardCharsets.UTF_8)));

                    return ApplyResponse.ofHeaders(Map.of("Authorization", List.of("Basic " + base64EncodedToken)));
                }));

    private ZendeskConnection() {
    }
}
