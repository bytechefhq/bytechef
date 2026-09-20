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

package com.bytechef.component.ssh.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.ssh.constant.SshConstants.HOST;
import static com.bytechef.component.ssh.constant.SshConstants.HOST_KEY_FINGERPRINT;
import static com.bytechef.component.ssh.constant.SshConstants.PASSPHRASE;
import static com.bytechef.component.ssh.constant.SshConstants.PASSWORD;
import static com.bytechef.component.ssh.constant.SshConstants.PORT;
import static com.bytechef.component.ssh.constant.SshConstants.PRIVATE_KEY;
import static com.bytechef.component.ssh.constant.SshConstants.USERNAME;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.component.ssh.util.SshClientUtils;

/**
 * @author Igor Beslic
 */
public class SshConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.BASIC_AUTH)
                .title("Username and Password")
                .properties(
                    string(HOST)
                        .label("Host")
                        .description("The hostname or IP address of the SSH server.")
                        .required(true),
                    integer(PORT)
                        .label("Port")
                        .description("The port number of the SSH server.")
                        .defaultValue(22)
                        .required(false),
                    string(USERNAME)
                        .label("Username")
                        .description("The username used to log in to the SSH server.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("The password of the user logging in to the SSH server.")
                        .controlType(Property.ControlType.PASSWORD)
                        .required(true),
                    string(HOST_KEY_FINGERPRINT)
                        .label("Host Key Fingerprint")
                        .description(
                            "The fingerprint of the SSH server's host key, for example " +
                                "SHA256:l5H0Bq1qGZaBb5Tn7s0AaCnNmCHiBBKvKPhTE7hrXTI. When left empty, any host key " +
                                "is accepted, which leaves the connection open to man-in-the-middle attacks.")
                        .required(false)),
            authorization(Authorization.AuthorizationType.CUSTOM)
                .title("Private Key")
                .properties(
                    string(HOST)
                        .label("Host")
                        .description("The hostname or IP address of the SSH server.")
                        .required(true),
                    integer(PORT)
                        .label("Port")
                        .description("The port number of the SSH server.")
                        .defaultValue(22)
                        .required(false),
                    string(USERNAME)
                        .label("Username")
                        .description("The username used to log in to the SSH server.")
                        .required(true),
                    string(PRIVATE_KEY)
                        .label("Private Key")
                        .description(
                            "The private key of the user logging in to the SSH server, in OpenSSH or PEM format, " +
                                "including the header and the footer line.")
                        .controlType(Property.ControlType.TEXT_AREA)
                        .required(true),
                    string(PASSPHRASE)
                        .label("Passphrase")
                        .description("The passphrase protecting the private key, if the key is encrypted.")
                        .controlType(Property.ControlType.PASSWORD)
                        .required(false),
                    string(HOST_KEY_FINGERPRINT)
                        .label("Host Key Fingerprint")
                        .description(
                            "The fingerprint of the SSH server's host key, for example " +
                                "SHA256:l5H0Bq1qGZaBb5Tn7s0AaCnNmCHiBBKvKPhTE7hrXTI. When left empty, any host key " +
                                "is accepted, which leaves the connection open to man-in-the-middle attacks.")
                        .required(false)))
        .test((connectionParameters, context) -> SshClientUtils.testConnection(connectionParameters));

    private SshConnection() {
    }
}
