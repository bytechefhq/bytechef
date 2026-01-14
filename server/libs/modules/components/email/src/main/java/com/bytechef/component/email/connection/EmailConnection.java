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

package com.bytechef.component.email.connection;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.email.constant.EmailConstants.CRYPTOGRAPHIC_PROTOCOL;
import static com.bytechef.component.email.constant.EmailConstants.HOST;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.email.constant.EmailConstants;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class EmailConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(HOST)
                .label("Host")
                .required(true),
            string(CRYPTOGRAPHIC_PROTOCOL)
                .controlType(Property.ControlType.SELECT)
                .label("Connection Security")
                .description(
                    "Connection security activates cryptographic protocol to secure communication over a network.")
                .options(
                    option(EmailConstants.TLS, EmailConstants.TLS,
                        "Transport Layer Security is the modern, more secure replacement for SSL"),
                    option(EmailConstants.SSL, EmailConstants.SSL,
                        "Secure Sockets Layer is an older protocol that has been deprecated due to security vulnerabilities."))
                .required(false))
        .authorizationRequired(false)
        .authorizations(
            authorization(AuthorizationType.BASIC_AUTH)
                .title("Basic Auth")
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true)));
}
