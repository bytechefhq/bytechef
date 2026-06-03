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

package com.bytechef.component.gaurus.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl;

/**
 * Provides the component connection definition.
 *
 * @generated
 *
 * @author Igor Beslic
 */
public class GaurusConnection {
    public static final ComponentDsl.ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .baseUri((connectionParameters, context) -> "https://bankconnect.gaurus.hr/api/client")
        .properties(string("baseUri").label("Service URI")
            .description("The base URI of the Gaurus service"))
        .authorizationRequired(true)
        .authorizations(authorization(Authorization.AuthorizationType.CUSTOM).title("Gaurus HmacSHA256 Authorization")
            .properties(
                string("clientId").label("Client ID")
                    .description("Client Id generated at GAURUS")
                    .required(true),
                string("clientSecret").label("Client Secret")
                    .description("The secret key for digital signing")
                    .required(true),
                bool("allowSelfSignedCert").label("Allow Self-Signed Certificates")
                    .description("Allow secure connections to servers with self-signed certificates")
                    .defaultValue(false))

        );

    private GaurusConnection() {
    }
}
