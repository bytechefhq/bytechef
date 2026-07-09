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

package com.bytechef.platform.security.web.mcp;

import com.bytechef.platform.configuration.domain.Environment;
import org.springaicommunity.mcp.security.server.apikey.ApiKey;

/**
 * The API key presented on an MCP request, together with the request context (URL path secret, requested environment)
 * needed by the per-server authentication providers.
 *
 * @author Ivica Cardic
 */
public class McpApiKeyCredentials implements ApiKey {

    private final Environment environment;
    private final String mcpServerSecretKey;
    private final String secretKey;

    public McpApiKeyCredentials(Environment environment, String mcpServerSecretKey, String secretKey) {
        this.environment = environment;
        this.mcpServerSecretKey = mcpServerSecretKey;
        this.secretKey = secretKey;
    }

    @Override
    public String getId() {
        return secretKey;
    }

    @Override
    public String getSecret() {
        return secretKey;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getMcpServerSecretKey() {
        return mcpServerSecretKey;
    }
}
