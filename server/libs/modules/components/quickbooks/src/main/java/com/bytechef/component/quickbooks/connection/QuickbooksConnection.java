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

package com.bytechef.component.quickbooks.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.BASE;
import static com.bytechef.component.quickbooks.constant.QuickbooksConstants.COMPANY_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import java.util.Map;

/**
 * @author Mario Cvjetojevic
 * @author Luka Ljubić
 * @author Monika Kušter
 */
public class QuickbooksConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> connectionParameters.getRequiredString(BASE) + "/v3/company/"
            + connectionParameters.getRequiredString(COMPANY_ID))
        .authorizations(
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(BASE)
                        .label("Base")
                        .description("The base URL for Quickbooks.")
                        .options(
                            option("Sandbox", "https://sandbox-quickbooks.api.intuit.com"),
                            option("Production", "https://quickbooks.api.intuit.com"))
                        .required(true),
                    string(COMPANY_ID)
                        .label("Company Id")
                        .description(
                            "To get the company id, go to your dashboard. On the top right corner press the gear " +
                                "logo and click Additional information. There you will see your company ID.")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true))
                .authorizationUrl((connection, context) -> "https://appcenter.intuit.com/connect/oauth2")
                .scopes((connection, context) -> Map.of("com.intuit.quickbooks.accounting", true))
                .tokenUrl((connection, context) -> "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer")
                .refreshUrl((connection, context) -> "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer"));

    private QuickbooksConnection() {
    }
}
