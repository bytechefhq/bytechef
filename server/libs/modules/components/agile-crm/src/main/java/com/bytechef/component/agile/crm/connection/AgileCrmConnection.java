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

package com.bytechef.component.agile.crm.connection;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.DOMAIN;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://%s.agilecrm.com/dev/api"
            .formatted(connectionParameters.getRequiredString(DOMAIN)))
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Authentication")
                .properties(
                    string(DOMAIN)
                        .label("Domain")
                        .description("https://DOMAIN.agilecrm.com")
                        .required(true),
                    string(USERNAME)
                        .label("Email")
                        .description("Email address of Agile CRM account.")
                        .required(true),
                    string(PASSWORD)
                        .label("REST API Key")
                        .description("Can be found in Admin settings -> Developers & API -> REST API Key.")
                        .required(true)));

    private AgileCrmConnection() {
    }
}
