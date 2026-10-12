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

package com.bytechef.platform.oauth2.authorizationserver.constant;

import java.time.Duration;
import java.util.List;

/**
 * @author Ivica Cardic
 */
public final class OAuth2AuthorizationServerConstants {

    public static final Duration MCP_ACCESS_TOKEN_TIME_TO_LIVE = Duration.ofHours(1);
    public static final String MCP_AUTOMATION_SCOPE = "mcp:automation";
    public static final String MCP_MANAGEMENT_SCOPE = "mcp:management";
    public static final Duration MCP_REFRESH_TOKEN_TIME_TO_LIVE = Duration.ofDays(30);
    public static final List<String> MCP_SCOPES = List.of(MCP_AUTOMATION_SCOPE, MCP_MANAGEMENT_SCOPE);
    public static final String TENANT_ID_CLAIM = "tenant_id";

    private OAuth2AuthorizationServerConstants() {
    }
}
