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
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.email.constant.EmailConstants.HOST;
import static com.bytechef.component.email.constant.EmailConstants.PORT;
import static com.bytechef.component.email.constant.EmailConstants.PROTOCOL;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.email.EmailProtocol;
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
            integer(PORT)
                .label("Port")
                .description("")
                .required(true)
                .defaultValue(25),
            string(PROTOCOL)
                .controlType(Property.ControlType.SELECT)
                .defaultValue(EmailProtocol.smtp.name())
                .label("Protocol")
                .description(
                    "Protocol defines communication procedure. SMTP allows sending emails, IMAP allows receiving emails. POP3 is older protocol for receiving emails.")
                .options(
                    option(EmailProtocol.smtp.name(), EmailProtocol.smtp.name(), "sending email"),
                    option(EmailProtocol.imap.name(), EmailProtocol.imap.name(), "receive email"),
                    option(EmailProtocol.pop3.name(), EmailProtocol.pop3.name(), "receive email"))
                .required(true),
            bool(EmailConstants.TLS)
                .label("Use TLS")
                .description("If selected the connection will use TLS when connecting to server."))
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
