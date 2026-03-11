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

package com.bytechef.component.mcp.client.connection;

import static com.bytechef.component.definition.Authorization.AUTHORIZATION_URL;
import static com.bytechef.component.definition.Authorization.CLIENT_ID;
import static com.bytechef.component.definition.Authorization.CLIENT_SECRET;
import static com.bytechef.component.definition.Authorization.HEADER_PREFIX;
import static com.bytechef.component.definition.Authorization.SCOPES;
import static com.bytechef.component.definition.Authorization.TOKEN;
import static com.bytechef.component.definition.Authorization.TOKEN_URL;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_SSE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.HTTP_STREAMABLE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.TRANSPORT_TYPE;
import static com.bytechef.component.mcp.client.constant.McpClientConstants.URL;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 */
public class McpClientConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(URL)
                .label("Server URL")
                .description("The URL of the MCP server to connect to.")
                .required(true),
            string(TRANSPORT_TYPE)
                .label("Transport Type")
                .description("The transport protocol to use for connecting to the MCP server.")
                .options(
                    option("HTTP Streamable", HTTP_STREAMABLE),
                    option("SSE", HTTP_SSE))
                .defaultValue(HTTP_STREAMABLE)
                .required(true))
        .authorizationRequired(false)
        .authorizations(
            authorization(AuthorizationType.BEARER_TOKEN)
                .title("Bearer Token")
                .properties(
                    string(TOKEN)
                        .label("Token")
                        .required(true)),
            authorization(AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                .title("OAuth2 Authorization Code")
                .properties(
                    string(AUTHORIZATION_URL)
                        .label("Authorization URL")
                        .required(true),
                    string(TOKEN_URL)
                        .label("Token URL")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(HEADER_PREFIX)
                        .label("Header Prefix")
                        .defaultValue(Authorization.BEARER),
                    string(SCOPES)
                        .label("Scopes")
                        .description("Optional comma-delimited list of scopes")
                        .controlType(ControlType.TEXT_AREA)),
            authorization(AuthorizationType.OAUTH2_CLIENT_CREDENTIALS)
                .title("OAuth2 Client Credentials")
                .properties(
                    string(TOKEN_URL)
                        .label("Token URL")
                        .required(true),
                    string(CLIENT_ID)
                        .label("Client Id")
                        .required(true),
                    string(CLIENT_SECRET)
                        .label("Client Secret")
                        .required(true),
                    string(HEADER_PREFIX)
                        .label("Header Prefix")
                        .defaultValue(Authorization.BEARER),
                    string(SCOPES)
                        .label("Scopes")
                        .description("Optional comma-delimited list of scopes")
                        .controlType(ControlType.TEXT_AREA)));

    private McpClientConnection() {
    }
}
