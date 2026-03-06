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

package com.bytechef.component.ftp.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.ftp.constant.FtpConstants.HOST;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSIVE_MODE;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSWORD;
import static com.bytechef.component.ftp.constant.FtpConstants.PORT;
import static com.bytechef.component.ftp.constant.FtpConstants.SFTP;
import static com.bytechef.component.ftp.constant.FtpConstants.USERNAME;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public class FtpConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .properties(
                    string(HOST)
                        .label("Host")
                        .description("The hostname or IP address of the FTP server.")
                        .required(true),
                    integer(PORT)
                        .label("Port")
                        .description("The port number of the FTP server.")
                        .defaultValue(21)
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("The username for authentication.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("The password for authentication.")
                        .required(true),
                    bool(PASSIVE_MODE)
                        .label("Passive Mode")
                        .description(
                            "Use passive mode for data connections. Recommended when the server is behind a firewall. "
                                +
                                "Only applicable for FTP connections.")
                        .defaultValue(true),
                    bool(SFTP)
                        .label("Use SFTP")
                        .description(
                            "Use SFTP (SSH File Transfer Protocol) instead of FTP. SFTP provides encrypted file " +
                                "transfer over SSH. When enabled, the port defaults to 22 instead of 21.")
                        .defaultValue(false)));

    private FtpConnection() {
    }
}
